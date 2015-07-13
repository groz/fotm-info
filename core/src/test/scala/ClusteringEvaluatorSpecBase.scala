import info.fotm.clustering.Defaults
import info.fotm.domain.Domain._
import info.fotm.domain._

import scala.collection.immutable.IndexedSeq

trait ClusteringEvaluatorSpecBase {
  val gen = Defaults.generators(3)
  import gen._

  val teamSize = Defaults.settings(3).teamSize
  val players1500: Seq[CharacterSnapshot] = (1 to teamSize).map(i => genPlayer)
  val players1580: Seq[CharacterSnapshot] = (1 to teamSize).map(i => genPlayer(1580))
  val player1500 = players1500.head
  val player1580 = players1580.head
  val team1500: Team = Team(players1500.map(_.id).toSet)
  val team1580: Team = Team(players1580.map(_.id).toSet)

  val ladder: CharacterLadder = CharacterLadder(Axis.all.head, (players1500 ++ players1580).map(p => (p.id, p)).toMap)
}
