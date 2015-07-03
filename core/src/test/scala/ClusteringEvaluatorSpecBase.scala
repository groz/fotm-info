import info.fotm.clustering.ClusteringEvaluatorData._
import info.fotm.domain.Domain._
import info.fotm.domain.Team

trait ClusteringEvaluatorSpecBase {
  val teamSize = 3
  val players1500 = (1 to teamSize).map(i => genPlayer)
  val players1580 = (1 to teamSize).map(i => genPlayer.copy(rating = 1580))
  val player1500 = players1500.head
  val player1580 = players1580.head
  val team1500 = Team(players1500.map(_.id).toSet)
  val team1580 = Team(players1580.map(_.id).toSet)

  val ladder: LadderSnapshot = (players1500 ++ players1580).map(p => (p.id, p)).toMap
}

