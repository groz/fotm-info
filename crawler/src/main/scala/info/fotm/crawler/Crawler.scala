package info.fotm.crawler

import info.fotm.api.BattleNetAPI
import info.fotm.api.models._
import info.fotm.api.regions._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object Crawler extends App {
  val apiKey = "vntnwpsguf4pqak7e8y7tgn35795fqfj"

  val api = new BattleNetAPI(US, apiKey).WoW

  case class CharId(name: String, realmSlug: String)

  type MyLeaderboard = Map[CharId, LeaderboardRow]

  var prev: MyLeaderboard = null

  def check(l1: MyLeaderboard, l2: MyLeaderboard) = {
    val commonKeys = l1.keySet.intersect(l2.keySet)

    val less = commonKeys.filter(k => l1(k).seasonTotal < l2(k).seasonTotal)
    val greater = commonKeys.filter(k => l1(k).seasonTotal > l2(k).seasonTotal)
    val equal = commonKeys.filter(k => l1(k).seasonTotal == l2(k).seasonTotal)

    println(s"Less: ${less.size}, Equal: ${equal.size}, Greater: ${greater.size}")

    val ltDiffs = less.map(k => l1(k).seasonWins + l1(k).seasonLosses - l2(k).seasonLosses - l2(k).seasonWins)
    val gtDiffs = greater.map(k => l1(k).seasonWins + l1(k).seasonLosses - l2(k).seasonLosses - l2(k).seasonWins)

    println(s"lt diffs: $ltDiffs")
    println(s"gt diffs: $gtDiffs")
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  while (true) {
    val leaderboardFuture: Future[Leaderboard] = api.leaderboard(Threes).recover {
      case _ => Leaderboard(List())
    }

    val leaderboard: Leaderboard = Await.result(leaderboardFuture, Duration.Inf)

    val current: MyLeaderboard = leaderboard.rows.map(r => (CharId(r.name, r.realmSlug), r)).toMap

    if (prev == null)
      prev = current

    if (current != prev) {
      println(current.size, prev.size)
      check(prev, current)
      prev = current
    }
  }
}



