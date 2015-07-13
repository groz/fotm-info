package info.fotm.crawler

import akka.actor.Actor
import akka.event.Logging
import info.fotm.clustering.{ClusteringEvaluator, CharFeatures, RealClusterer}
import info.fotm.crawler.TeamFinderActor.UpdateFound
import info.fotm.domain._

class TeamFinderActor(algo: RealClusterer) extends Actor {
  val log = Logging(context.system, this)

  override def receive = {
    case UpdateFound(bracketSize, charDiffs: Set[CharFeatures], currentLadder: CharacterLadder) =>
      val vectorizedFeatures = charDiffs.map(c => (c, ClusteringEvaluator.featurize(c))).toMap

      val teamUpdates: Set[TeamUpdate] = for {
        cluster: Seq[CharFeatures] <- algo.clusterize(vectorizedFeatures, bracketSize)
      } yield {
        val won = cluster.head.won
        val cs = cluster.map(_.id).map(c => currentLadder.rows(c)).toSet
        val snapshot = TeamSnapshot(cs)
        TeamUpdate(snapshot.team, won)
      }

      log.info(s"Found: $teamUpdates")

      sender ! teamUpdates
  }
}

case object TeamFinderActor {
  case class UpdateFound(bracketSize: Int, diffFeatures: Set[CharFeatures], currentLadder: CharacterLadder)
}
