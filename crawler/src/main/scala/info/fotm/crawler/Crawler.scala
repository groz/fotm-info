package info.fotm.crawler

import akka.actor.Actor
import info.fotm.api.BattleNetAPI
import info.fotm.api.models._
import info.fotm.api.regions._

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object CrawlerActor {
  case object Crawl
}

class CrawlerActor(apiKey: String, region: Region, bracket: Bracket) extends Actor {
  import CrawlerActor._
  import Crawler._

  val api = new BattleNetAPI(region, apiKey).WoW

  val lbOrdering = Ordering.fromLessThan[MyLeaderboard] { (l1, l2) =>
    val commonKeys = l1.keySet.intersect(l2.keySet)
    val lt = commonKeys.exists(k => l1(k).seasonTotal < l2(k).seasonTotal)
    val gt = commonKeys.exists(k => l1(k).seasonTotal > l2(k).seasonTotal)

    if (lt && gt)
      throw new Exception("ladders can't be compared")

    lt
  }

  var history = mutable.TreeSet.empty(lbOrdering)
  val maxSize = 10

  def receive: Receive = {
    case Crawl =>
      print(".")
      val leaderboardFuture = api.leaderboard(Twos).map { leaderboard =>
        val current: MyLeaderboard = leaderboard.rows.map(r => (CharId(r.name, r.realmSlug), r)).toMap

        if (history.add(current)) {
          println()
          // show position in history
          val before = history.until(current).toIndexedSeq.map(_ => "_").mkString
          val after = history.from(current).toIndexedSeq.tail.map(_ => "_").mkString
          println(s"\n${before}X${after}")

          println(s"History size: ${history.size}")

          val prev = history.until(current).lastOption
          prev.filter(distance(_, current) == 1).foreach { check(_, current) }

          val next = history.from(current).tail.headOption
          next.filter(distance(current, _) == 1).foreach { check(current, _) }

          // TODO: keep history to fixed max size
          if (history.size > maxSize)
            history -= history.head
        }
      }.recover {
        case _ => print("F")
      }

  }

}

object Crawler extends App {
  val apiKey = "vntnwpsguf4pqak7e8y7tgn35795fqfj"

  case class CharId(name: String, realmSlug: String)
  type MyLeaderboard = Map[CharId, LeaderboardRow]

  def check(previous: MyLeaderboard, current: MyLeaderboard) = {
    println()
    val commonKeys: Set[CharId] = previous.keySet.intersect(current.keySet)

    // stats
    val changed: Set[CharId] = commonKeys.filter(k => previous(k).seasonTotal != current(k).seasonTotal)
    println(s"Total: ${current.size}, changed: ${changed.size}")

    val diffs: Set[Int] = changed.map(k => previous(k).seasonTotal - current(k).seasonTotal)
    println(s"diffs: $diffs")

    // only take those with 1 game diff
    val focus: Set[CharId] = changed.filter(k => previous(k).seasonTotal + 1 == current(k).seasonTotal)
    val gs = focus.groupBy(k => current(k).factionId).map(kv => kv._1 -> kv._2.size)
    println(s"Factions: $gs")

    val (winners, losers) = focus.partition(k => previous(k).seasonWins < current(k).seasonWins)
    println(s"focus: ${focus.size}, winners: ${winners.size}, losers: ${losers.size}")
  }

  def distance(l1: MyLeaderboard, l2: MyLeaderboard): Int = {
    val commonKeys: Set[CharId] = l1.keySet.intersect(l2.keySet)
    val distances: Set[Int] = commonKeys.map(k => l2(k).seasonTotal - l1(k).seasonTotal)
    println(s"Distances: $distances")
    distances.map(Math.abs).max
  }
}
