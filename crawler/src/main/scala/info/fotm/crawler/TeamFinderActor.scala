package info.fotm.crawler

import akka.actor.Actor
import akka.event.Logging
import info.fotm.clustering.{ClusteringEvaluator, CharFeatures, RealClusterer}
import info.fotm.crawler.TeamFinderActor.UpdateFound
import info.fotm.domain.{TeamStats, Team}

import scala.collection.mutable

class TeamFinderActor(algo: RealClusterer) extends Actor {
  val log = Logging(context.system, this)

  val teamLadder: mutable.Map[Team, TeamStats] =
    mutable.HashMap.empty[Team, TeamStats].withDefaultValue(TeamStats.empty)

  override def receive = {
    case UpdateFound(bracketSize, charDiffs: Set[CharFeatures]) =>
      val vectorizedFeatures = charDiffs.map(c => (c, ClusteringEvaluator.featurize(c))).toMap

      for { cluster <- algo.clusterize(vectorizedFeatures, bracketSize) } {
        val team = Team(cluster.map(_.id).toSet)
        val won = cluster.head.won
        val stats = teamLadder(team)
        teamLadder(team) = if (won) stats.win else stats.lose
      }

      log.info(s"Total: ${teamLadder.size}, teams: $teamLadder")
  }
}

case object TeamFinderActor {
  case class UpdateFound(bracketSize: Int, diffFeatures: Set[CharFeatures])
}