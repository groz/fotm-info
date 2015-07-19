package info.fotm.crawler

import akka.actor.{Actor, ActorRef}
import akka.event.{Logging, LoggingAdapter, LoggingReceive}
import akka.pattern.pipe
import info.fotm.aether.Storage
import info.fotm.api.BattleNetAPI
import info.fotm.api.models._
import info.fotm.clustering._
import info.fotm.clustering.implementations.HTClusterer3
import info.fotm.domain._
import info.fotm.util.ObservableReadStream

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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

  private def hydrateTeam(ladderUpdate: LadderUpdate, team: Team): TeamUpdate = {
    val id = team.members.head
    val won = ladderUpdate.statsUpdates.find(_.id == id).get.won

    val view = TeamView(team.members.map(ladderUpdate.current.rows))
    TeamUpdate(view, won)
  }

  val evaluator = new ClusteringEvaluator(FeatureSettings.features)
  val clusterer = RealClusterer.wrap(new HTClusterer3)

  val updatesObserver = new UpdatesQueue[CharacterLadder](historySize)

  // subscribe storage to ladder updates
  val teamsFound: ObservableReadStream[(LadderUpdate, Set[TeamUpdate])] =
    for {
      (prev, next) <- updatesObserver
      ladderUpdate = LadderUpdate(prev, next)
      if ladderUpdate.distance == 1
      teams = evaluator.findTeamsInUpdate(ladderUpdate, clusterer)
      if teams.nonEmpty
      teamUpdates = teams.map(hydrateTeam(ladderUpdate, _))
    } yield (ladderUpdate, teamUpdates)

  val updatesSubscription =
    for {(ladderUpdate, teamUpdates) <- teamsFound} {
      log.debug(s"Sending ${teamUpdates.size} teams to storage...")
      storage ! Storage.Updates(axis, teamUpdates.toSeq, ladderUpdate.charDiffs)
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
}
