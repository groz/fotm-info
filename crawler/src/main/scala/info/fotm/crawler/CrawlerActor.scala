package info.fotm.crawler

import info.fotm.aether.Storage
import info.fotm.api.BattleNetAPI
import info.fotm.api.models._
import info.fotm.clustering.RMClustering.EqClusterer2
import info.fotm.clustering.enhancers.{Summator, Verifier, Multiplexer, ClonedClusterer}
import info.fotm.clustering._
import info.fotm.domain._

import akka.actor.{ActorSelection, ActorRef, Actor}
import akka.event.{LoggingAdapter, Logging}
import akka.pattern.pipe
import scala.concurrent.ExecutionContext.Implicits.global

object CrawlerActor {
  case object Crawl
  case class LeaderboardReceived(leaderboard: Leaderboard)
  case object CrawlFailed
}

class CrawlerActor(storage: ActorSelection, apiKey: String, axis: Axis) extends Actor {
  import CrawlerActor._

  implicit val log: LoggingAdapter = Logging(context.system, this)
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
      new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer
    ) with Verifier
  )

  val clusterer = algos("RM")

  val updatesObserver = new UpdatesQueue[CharacterLadder](10)

  // subscribe storage to ladder updates
  val updatesSubscription = for {
    (prev, next) <- updatesObserver.stream
    if distance(prev, next) == 1
    teams = processUpdate(prev, next)
  } {
    log.debug(s"Sending ${teams.size} updates to storage...")
    storage ! Storage.Updates(axis, teams)
  }

  def crawling(recrawlRequested: Boolean): Receive = {
    case CrawlFailed =>
      context.unbecome()
      if (recrawlRequested)
        self ! Crawl

    case LeaderboardReceived(leaderboard: Leaderboard) =>
      context.unbecome()
      if (recrawlRequested)
        self ! Crawl

      val current = CharacterLadder(axis, leaderboard)
      updatesObserver.process(current)

    case Crawl =>
      context become crawling(true)
  }

  override def receive: Receive = {
    case Crawl =>
      context.become(crawling(false))

      api.leaderboard(axis.bracket)
        .map(LeaderboardReceived)
        .recover { case _ => CrawlFailed } pipeTo self
  }

  private def processUpdate(previousLadder: CharacterLadder, currentLadder: CharacterLadder) = {
    val commonKeys: Set[CharacterId] = previousLadder.rows.keySet.intersect(currentLadder.rows.keySet)

    // stats
    val changed: Set[CharacterId] = commonKeys.filter(k => previousLadder(k).season.total != currentLadder(k).season.total)

    // only take those with 1 game diff
    val focus: Set[CharacterId] = changed.filter(k => previousLadder(k).season.total + 1 == currentLadder(k).season.total)
    val factions: Map[Int, Set[CharacterId]] = focus.groupBy(k => currentLadder.rows(k).view.factionId)
    val factionNumbers: Map[Int, Int] = factions.map(g => (g._1, g._2.size))

    // stats logging
    val diffs: Set[Int] = changed.map(k => previousLadder(k).season.total - currentLadder(k).season.total)

    log.debug(s"Total: ${currentLadder.rows.size}, changed: ${changed.size}")
    log.debug(s"diffs: $diffs")
    log.debug(s"Factions: $factionNumbers")

    for {
      (_, factionChars: Set[CharacterId]) <- factions.toList    // split by factions
      features: Set[CharFeatures] = extractFeatures(factionChars, previousLadder, currentLadder)
      (_, bucket: Set[CharFeatures]) <- features.groupBy(_.won) // further split by winners/losers
      team <- findTeams(bucket, currentLadder)                  // find teams for each group
    } yield team
  }

  def extractFeatures(chars: Set[CharacterId], previousLadder: CharacterLadder, currentLadder: CharacterLadder) =
    chars.map { p => CharFeatures(p, previousLadder(p), currentLadder(p)) }

  private def findTeams(charDiffs: Set[CharFeatures], currentLadder: CharacterLadder): Set[TeamUpdate] = {
    val vectorizedFeatures = charDiffs.map(c => (c, ClusteringEvaluator.featurize(c))).toMap

    val teamUpdates: Set[TeamUpdate] = for {
      cluster: Seq[CharFeatures] <- clusterer.clusterize(vectorizedFeatures, axis.bracket.size)
    } yield {
        val won = cluster.head.won
        val cs = cluster.map(_.id).map(c => currentLadder.rows(c)).toSet
        val snapshot = TeamSnapshot(cs)
        TeamUpdate(snapshot.view, won)
      }

    log.info(s"Found: $teamUpdates")

    teamUpdates
  }

  private def distance(l1: CharacterLadder, l2: CharacterLadder): Int = {
    val commonKeys: Set[CharacterId] = l1.rows.keySet.intersect(l2.rows.keySet)
    val distances: Set[Int] = commonKeys.map(k => l2(k).season.total - l1(k).season.total)
    log.debug(s"Distances: $distances")
    distances.map(Math.abs).max
  }
}
