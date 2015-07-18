import info.fotm.clustering.Defaults
import info.fotm.domain._
import scala.collection.breakOut

trait ClusteringEvaluatorSpecBase {
  val gen = Defaults.generators(3)
  import gen._

  val teamSize = Defaults.settings(3).teamSize
  val players1500: Seq[CharacterSnapshot] = (1 to teamSize).map(i => genPlayer(1500, 1))
  val players1580: Seq[CharacterSnapshot] = (1 to teamSize).map(i => genPlayer(1580, 1))
  val player1500 = players1500.head
  val player1580 = players1580.head
  val team1500: Team = Team(players1500.map(_.id).toSet)
  val team1580: Team = Team(players1580.map(_.id).toSet)

  val hPlayers1500: Seq[CharacterSnapshot] = (1 to teamSize).map(i => genPlayer(1500, 0))
  val hPlayers1580: Seq[CharacterSnapshot] = (1 to teamSize).map(i => genPlayer(1580, 0))

  val hTeam1500: Team = Team(hPlayers1500.map(_.id).toSet)
  val hTeam1580: Team = Team(hPlayers1580.map(_.id).toSet)

  val aPlayers = players1500 ++ players1580
  val hPlayers = hPlayers1500 ++ hPlayers1580
  val allPlayers: Seq[CharacterSnapshot] = aPlayers ++ hPlayers

  val allTeams = Seq(team1500, team1580, hTeam1500, hTeam1580)

  val ladder: CharacterLadder = CharacterLadder(Axis.all.head, allPlayers.map(p => (p.id, p)).toMap)
}
