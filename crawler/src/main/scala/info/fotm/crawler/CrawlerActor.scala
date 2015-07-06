package info.fotm.crawler

import akka.actor.{Props, Actor}
import akka.event.{LoggingReceive, Logging}
import info.fotm.api.BattleNetAPI
import info.fotm.api.models._
import info.fotm.api.regions._
import info.fotm.clustering.enhancers.{Summator, Verifier, Multiplexer, ClonedClusterer}
import info.fotm.clustering._
import info.fotm.domain.{CharacterStats, CharacterId}

import scala.collection.mutable
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

  val lbOrdering = Ordering.fromLessThan[MyLeaderboard] { (l1, l2) =>
    val commonKeys = l1.keySet.intersect(l2.keySet)
    val lt = commonKeys.exists(k => l1(k).seasonTotal < l2(k).seasonTotal)
    val gt = commonKeys.exists(k => l1(k).seasonTotal > l2(k).seasonTotal)

    if (lt && gt)
      throw new Exception("ladders can't be compared")

    lt
  }

  val algos: Map[String, RealClusterer] = Map(
    "HTClusterer2" -> RealClusterer.wrap(new HTClusterer2),
    "HTClusterer2_Verifier" -> new ClonedClusterer(RealClusterer.wrap(new HTClusterer2)) with Verifier,
    "Closest_Multiplexer_Verifier" -> new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer with Verifier,
    "HT2_Closest_Multiplexer_Verifier" -> new ClonedClusterer(new Summator(
      RealClusterer.wrap(new HTClusterer2),
      new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer
    )) with Verifier
  )

  val history = mutable.TreeSet.empty(lbOrdering)
  val maxSize = 10
  val finders = (for {
    (name, algo: RealClusterer) <- algos
  } yield context.actorOf(Props(classOf[TeamFinderActor], algo), self.path.name + "-finder-"+name))
  .toList

  override def receive = {
    case Crawl =>
      api.leaderboard(Twos).map(LeaderboardReceived).recover {
        case _ => CrawlFailed
      } pipeTo self

    case CrawlFailed =>

    case LeaderboardReceived(leaderboard: Leaderboard) =>
      val current: MyLeaderboard = leaderboard.rows.map(r => (CharacterId(r.name, r.realmSlug), r)).toMap

      if (history.add(current)) {
        val before = history.until(current).toIndexedSeq.map(_ => "_").mkString
        val after = history.from(current).toIndexedSeq.tail.map(_ => "_").mkString

        log.debug(s"Leaderboard history queue: ${before}X${after}")

        val prev = history.until(current).lastOption
        prev.filter(distance(_, current) == 1).foreach {
          processUpdate(_, current)
        }

        val next = history.from(current).tail.headOption
        next.filter(distance(current, _) == 1).foreach {
          processUpdate(current, _)
        }

        if (history.size > maxSize)
          history -= history.head
      }
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

  private def distance(l1: MyLeaderboard, l2: MyLeaderboard): Int = {
    val commonKeys: Set[CharacterId] = l1.keySet.intersect(l2.keySet)
    val distances: Set[Int] = commonKeys.map(k => l2(k).seasonTotal - l1(k).seasonTotal)
    log.debug(s"Distances: $distances")
    distances.map(Math.abs).max
  }
}
