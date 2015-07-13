package info.fotm.crawler

import akka.actor.{ActorRef, Props, Actor}
import akka.event.{LoggingAdapter, LoggingReceive, Logging}
import info.fotm.api.BattleNetAPI
import info.fotm.api.models._
import info.fotm.api._
import info.fotm.clustering.RMClustering.EqClusterer2
import info.fotm.clustering.enhancers.{Summator, Verifier, Multiplexer, ClonedClusterer}
import info.fotm.clustering._
import info.fotm.domain.{Axis, CharacterLadder, CharacterStats, CharacterId}

import akka.pattern.pipe

import scala.concurrent.ExecutionContext.Implicits.global

object CrawlerActor {
  case object Crawl
  case class LeaderboardReceived(leaderboard: Leaderboard)
  case object CrawlFailed
}

class CrawlerActor(apiKey: String, axis: Axis) extends Actor {
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
    "HT2_CM_RM_V" -> new Summator(
      RealClusterer.wrap(new HTClusterer2),
      RealClusterer.wrap(new EqClusterer2),
      new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer
    ) with Verifier
  )

  val updatesObserver = new UpdatesQueue[CharacterLadder](10)

  val subs = for {
    (prev, next) <- updatesObserver.stream
    if distance(prev, next) == 1
  } processUpdate(prev, next)

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

  private def processUpdate(previous: CharacterLadder, current: CharacterLadder) = {
    val commonKeys: Set[CharacterId] = previous.rows.keySet.intersect(current.rows.keySet)

    // stats
    val changed: Set[CharacterId] = commonKeys.filter(k => previous(k).season.total != current(k).season.total)

    // only take those with 1 game diff
    val focus: Set[CharacterId] = changed.filter(k => previous(k).season.total + 1 == current(k).season.total)
    val factions: Map[Int, Set[CharacterId]] = focus.groupBy(k => current.rows(k).view.factionId)
    val factionNumbers: Map[Int, Int] = factions.map(g => (g._1, g._2.size))

    // stats logging
    val diffs: Set[Int] = changed.map(k => previous(k).season.total - current(k).season.total)

    log.debug(s"Total: ${current.rows.size}, changed: ${changed.size}")
    log.debug(s"diffs: $diffs")
    log.debug(s"Factions: $factionNumbers")

    // used to calc current rating

    // send to finders
    for {
      (_, factionChars: Set[CharacterId]) <- factions
      features: Set[CharFeatures] = factionChars.map { p =>
        CharFeatures(p, previous(p), current(p))
      }
      (_, bucket: Set[CharFeatures]) <- features.groupBy(_.won)
    } {
      finders.foreach { _ ! TeamFinderActor.UpdateFound(axis.bracket.size, bucket, current) }
    }
  }

  private def distance(l1: CharacterLadder, l2: CharacterLadder): Int = {
    val commonKeys: Set[CharacterId] = l1.rows.keySet.intersect(l2.rows.keySet)
    val distances: Set[Int] = commonKeys.map(k => l2(k).season.total - l1(k).season.total)
    log.debug(s"Distances: $distances")
    distances.map(Math.abs).max
  }
}
