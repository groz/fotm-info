package info.fotm.crawler

import akka.actor.{ActorRef, Props, Actor}
import akka.event.{LoggingReceive, Logging}
import info.fotm.api.BattleNetAPI
import info.fotm.api.models._
import info.fotm.api.regions._
import info.fotm.clustering.RMClustering.EqClusterer2
import info.fotm.clustering.enhancers.{Summator, Verifier, Multiplexer, ClonedClusterer}
import info.fotm.clustering._
import info.fotm.domain.{CharacterStats, CharacterId}

import akka.pattern.pipe

import scala.concurrent.ExecutionContext.Implicits.global

object CrawlerActor {
  type MyLeaderboard = Map[CharacterId, LeaderboardRow]

  case object Crawl
  case class LeaderboardReceived(leaderboard: Leaderboard)
  case object CrawlFailed

  def toStats(row: LeaderboardRow) =
    CharacterStats(
      id = CharacterId(row.name, row.realmSlug),
      rating = row.rating,
      weeklyWins = row.weeklyWins,
      weeklyLosses = row.weeklyLosses,
      seasonWins = row.seasonWins,
      seasonLosses = row.seasonLosses
    )
}

class CrawlerActor(apiKey: String, region: Region, bracket: Bracket) extends Actor {
  import CrawlerActor._

  val log = Logging(context.system, this)
  val api = new BattleNetAPI(region, apiKey).WoW

  implicit val lbOrdering = Ordering.fromLessThan[MyLeaderboard] { (l1, l2) =>
    val commonKeys = l1.keySet.intersect(l2.keySet)
    val lt = commonKeys.exists(k => l1(k).seasonTotal < l2(k).seasonTotal)
    val gt = commonKeys.exists(k => l1(k).seasonTotal > l2(k).seasonTotal)

    if (lt && gt)
      throw new Exception("ladders can't be compared")

    lt
  }

  val algos: Map[String, RealClusterer] = Map(
    "CM_V" -> new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer with Verifier,
    "RM_V" -> new ClonedClusterer(RealClusterer.wrap(new EqClusterer2)) with Verifier,
    "HT2_CM_RM_V" -> new Summator(
      RealClusterer.wrap(new HTClusterer2),
      RealClusterer.wrap(new EqClusterer2),
      new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer
    ) with Verifier
  )

  val updatesObserver = new ConsecutiveUpdatesObserver[MyLeaderboard](10)//.filter(distance(_) == 1)

  val finders: List[ActorRef] = (
    for {
      (name, algo: RealClusterer) <- algos
    } yield {
      context.actorOf(Props(classOf[TeamFinderActor], algo), name)
    }).toList

  def crawling(recrawlRequested: Boolean): Receive = {
    case CrawlFailed =>
      context.unbecome()
      if (recrawlRequested)
        self ! Crawl

    case LeaderboardReceived(leaderboard: Leaderboard) =>
      context.unbecome()
      if (recrawlRequested)
        self ! Crawl

      val current: MyLeaderboard = leaderboard.rows.map(r => (CharacterId(r.name, r.realmSlug), r)).toMap
      updatesObserver.process(current)

    case Crawl =>
      context become crawling(true)
  }

  override def receive: Receive = {
    case Crawl =>
      context.become(crawling(false))

      api.leaderboard(Twos)
        .map(LeaderboardReceived)
        .recover { case _ => CrawlFailed } pipeTo self
  }

  private def processUpdate(previous: MyLeaderboard, current: MyLeaderboard) = {
    val commonKeys: Set[CharacterId] = previous.keySet.intersect(current.keySet)

    // stats
    val changed: Set[CharacterId] = commonKeys.filter(k => previous(k).seasonTotal != current(k).seasonTotal)

    // only take those with 1 game diff
    val focus: Set[CharacterId] = changed.filter(k => previous(k).seasonTotal + 1 == current(k).seasonTotal)
    val factions: Map[Int, Set[CharacterId]] = focus.groupBy(k => current(k).factionId)
    val factionNumbers: Map[Int, Int] = factions.map(g => (g._1, g._2.size))

    // stats logging
    val diffs: Set[Int] = changed.map(k => previous(k).seasonTotal - current(k).seasonTotal)

    log.debug(s"Total: ${current.size}, changed: ${changed.size}")
    log.debug(s"diffs: $diffs")
    log.debug(s"Factions: $factionNumbers")

    // send to finders
    for {
      (_, factionChars: Set[CharacterId]) <- factions
      features: Set[CharFeatures] = factionChars.map(p => CharFeatures(toStats(previous(p)), toStats(current(p))))
      (_, bucket: Set[CharFeatures]) <- features.groupBy(_.won)
    } {
      finders.foreach { _ ! TeamFinderActor.UpdateFound(bracket.size, bucket) }
    }
  }

  private def distance(kv: (MyLeaderboard, MyLeaderboard)): Int = {
    val (l1, l2) = kv
    val commonKeys: Set[CharacterId] = l1.keySet.intersect(l2.keySet)
    val distances: Set[Int] = commonKeys.map(k => l2(k).seasonTotal - l1(k).seasonTotal)
    log.debug(s"Distances: $distances")
    distances.map(Math.abs).max
  }
}
