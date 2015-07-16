package info.fotm.crawler

import info.fotm.aether.Storage
import info.fotm.api.BattleNetAPI
import info.fotm.api.models._
import info.fotm.clustering.implementations.{ClosestClusterer, HTClusterer2}
import info.fotm.clustering.implementations.RMClustering.EqClusterer2
import info.fotm.clustering.enhancers.{Summator, Verifier, Multiplexer, ClonedClusterer}
import info.fotm.clustering._
import info.fotm.domain._

import akka.actor.{ActorSelection, ActorRef, Actor}
import akka.event.{LoggingReceive, LoggingAdapter, Logging}
import akka.pattern.pipe
import info.fotm.util.ObservableReadStream
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

object CrawlerActor {
  case object Crawl
  case class LeaderboardReceived(leaderboard: Leaderboard)
  case object CrawlFailed
  case object CrawlTimedOut
}

class CrawlerActor(storage: ActorRef, apiKey: String, axis: Axis) extends Actor {
  import CrawlerActor._

  val historySize = 20

  implicit val log: LoggingAdapter = Logging(context.system, this)

  val requestTimeout = 30.seconds

  val api = new BattleNetAPI(axis.region, apiKey).WoW

  implicit val lbOrdering = Ordering.fromLessThan[CharacterLadder] { (l1, l2) =>
    val ids = l1.rows.keySet.intersect(l2.rows.keySet)
    val lt = ids.exists(id => l1(id).season.total < l2(id).season.total)
    val gt = ids.exists(id => l1(id).season.total > l2(id).season.total)

    if (lt && gt)
      throw new Exception("ladders can't be compared")

    lt
  }

  val algos: Map[String, RealClusterer] = Map(
    "CM_V" -> new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer with Verifier,
    "RM_V" -> new ClonedClusterer(RealClusterer.wrap(new EqClusterer2)) with Verifier,
    "RM" -> RealClusterer.wrap(new EqClusterer2),
    "HT2_CM_RM_V" -> new Summator(
      RealClusterer.wrap(new HTClusterer2),
      RealClusterer.wrap(new EqClusterer2),
      new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer {
        override lazy val multiplexTurns = 30
        override lazy val multiplexThreshold = 5
      }
    ) with Verifier
  )

  val clusterer = algos("HT2_CM_RM_V")

  val updatesObserver = new UpdatesQueue[CharacterLadder](historySize)

  // subscribe storage to ladder updates
  val updatesSubscription = for {
    (prev, next) <- updatesObserver
    ladderUpdate = LadderUpdate(prev, next)
    if ladderUpdate.distance == 1
    teamUpdates: List[TeamUpdate] = processUpdate(ladderUpdate)
    if teamUpdates.nonEmpty
  } {
    log.debug(s"Sending ${teamUpdates.size} teams to storage...")
    storage ! Storage.Updates(axis, teamUpdates, ladderUpdate.charDiffs)
  }

  def continue(recrawlRequested: Boolean, message: String = "") = {
    log.debug(s"continue with $message, recrawl: $recrawlRequested")

    context.unbecome()
    if (recrawlRequested)
      self ! Crawl
  }

  def crawling(recrawlRequested: Boolean): Receive = LoggingReceive {
    case CrawlTimedOut =>
      continue(recrawlRequested, "Crawl timed out")

    case CrawlFailed =>
      continue(recrawlRequested, "Crawl failed")

    case LeaderboardReceived(leaderboard: Leaderboard) =>
      val current = CharacterLadder(axis, leaderboard)
      updatesObserver.process(current)
      continue(recrawlRequested)

    case Crawl =>
      context become crawling(true)
  }

  override def receive: Receive = LoggingReceive {
    case Crawl =>
      context.become(crawling(false))

      val delay = akka.pattern.after(requestTimeout, using = context.system.scheduler) {
        Future.successful(CrawlTimedOut)
      }

      val query = api.leaderboard(axis.bracket)
        .map(LeaderboardReceived)
        .recover { case _ => CrawlFailed }

      Future firstCompletedOf Seq(delay, query) pipeTo self
  }

  private def processUpdate(ladderUpdate: LadderUpdate): List[TeamUpdate] = {
    import ladderUpdate._
    // only take those with 1 game diff
    val changed: Set[CharacterId] = commonIds.filter(k => previous(k).season.total + 1 == current(k).season.total)
    val factions: Map[Int, Set[CharacterId]] = changed.groupBy(k => current.rows(k).view.factionId)
    val factionNumbers: Map[Int, Int] = factions.map(g => (g._1, g._2.size))

    // stats logging
    log.debug(s"Total: ${current.rows.size}, changed: ${changed.size}")
    log.debug(s"Factions: $factionNumbers")
    log.debug(s"Distances: ${ladderUpdate.distances}")

    // output teams
    for {
      (_, factionChars: Set[CharacterId]) <- factions.toList      // split by factions
      features: Set[CharFeatures] = extractFeatures(factionChars, previous, current)
      (_, bucket: Set[CharFeatures]) <- features.groupBy(_.won)   // further split by winners/losers
      team <- findTeams(bucket, current)                          // find teams for each group
    } yield team
  }

  def extractFeatures(chars: Set[CharacterId], previousLadder: CharacterLadder, currentLadder: CharacterLadder) =
    chars.map { p => CharFeatures(p, previousLadder(p), currentLadder(p)) }

  private def findTeams(charDiffs: Set[CharFeatures], currentLadder: CharacterLadder): Set[TeamUpdate] = {
    val vectorizedFeatures = charDiffs.map(c => (c, ClusteringEvaluator.featurize(c))).toMap

    val result = for {
      cluster: Seq[CharFeatures] <- clusterer.clusterize(vectorizedFeatures, axis.bracket.size)
    } yield {
      val won = cluster.head.won
      val cs = cluster.map(_.id).map(c => currentLadder.rows(c)).toSet
      val snapshot = TeamSnapshot(cs)
      TeamUpdate(snapshot.view, won)
    }

    log.debug(s"Teams found: ${result.size}")

    result
  }
}
