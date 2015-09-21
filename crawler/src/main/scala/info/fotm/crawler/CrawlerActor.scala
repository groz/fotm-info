package info.fotm.crawler

import akka.actor.Actor
import akka.event.{Logging, LoggingAdapter, LoggingReceive}
import akka.pattern.pipe
import info.fotm.aether.{AetherConfig, FotmStorage, MongoFotmStorage}
import info.fotm.api.models._
import info.fotm.clustering._
import info.fotm.clustering.enhancers.{ClonedClusterer, SimpleMultiplexer}
import info.fotm.clustering.implementations.ClosestClusterer
import info.fotm.domain._
import info.fotm.util.ObservableReadStream

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CrawlerActor {

  case object Crawl

  case class LeaderboardReceived(leaderboard: Leaderboard)

  case object CrawlFailed

  case object CrawlTimedOut

  def createClusterer: RealClusterer =
    new ClonedClusterer(RealClusterer.sequence(
      new SimpleMultiplexer(new ClosestClusterer, 20, 10).toReal,
      new SimpleMultiplexer(new ClosestClusterer, 10, 4).toReal,
      new SimpleMultiplexer(new ClosestClusterer, 10, 2).toReal
    )) with SeenEnhancer

  implicit val lbOrdering = Ordering.fromLessThan[CharacterLadder] { (l1, l2) =>
    val ids = l1.rows.keySet.intersect(l2.rows.keySet)
    val lt = ids.exists(id => l1(id).season.total < l2(id).season.total)
    val gt = ids.exists(id => l1(id).season.total > l2(id).season.total)

    if (lt && gt)
      throw new Exception("ladders can't be compared")

    lt
  }
}

class CrawlerActor(fetchLeaderboard: () => Future[Leaderboard], axis: Axis) extends Actor {

  import CrawlerActor._

  val clusterer = createClusterer

  val historySize = 12

  implicit val log: LoggingAdapter = Logging(context.system, this)

  private def hydrateTeam(ladderUpdate: LadderUpdate, team: Team): TeamUpdate = {
    val id = team.members.head
    val won = ladderUpdate.statsUpdates.find(_.id == id).get.won

    val view = TeamView(team.members.map(ladderUpdate.current.rows))
    TeamUpdate(view, won)
  }

  val evaluator = new ClusteringEvaluator(FeatureSettings.features)

  val updatesObserver = new UpdatesQueue[CharacterLadder](historySize)

  // subscribe storage to ladder updates
  val ladderUpdates: ObservableReadStream[LadderUpdate] =
    for {
      (prev, next) <- updatesObserver
      ladderUpdate = LadderUpdate(prev, next)
      if ladderUpdate.distance == 1
    } yield ladderUpdate

  val fotmStorage = new MongoFotmStorage

  val updatesSubscription =
    for (ladderUpdate <- ladderUpdates) {
      val teams = evaluator.findTeamsInUpdate(ladderUpdate, clusterer)
      val teamUpdates = teams.map(t => hydrateTeam(ladderUpdate, t))

      log.debug(s"Sending ${teamUpdates.size} teams and ${ladderUpdate.charDiffs.size} chars to storage...")

      val updates = FotmStorage.Updates(axis, teamUpdates.toSeq, ladderUpdate.charDiffs)
      fotmStorage.update(updates)
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

      val query =
        fetchLeaderboard()
        .map(LeaderboardReceived)
        .recover {
          case ex: Throwable =>
            log.error(s"$ex")
            CrawlFailed
        }

      pipe(query) to self
  }
}
