package info.fotm.domain

final case class TeamSnapshot private (team: Team, snapshots: Set[CharacterSnapshot], rating: Int, stats: Stats)

object TeamSnapshot {
  def apply(snapshots: Set[CharacterSnapshot]): TeamSnapshot = {
    val team = Team(snapshots.map(_.id))
    val totalRating = snapshots.toList.map(_.stats.rating)
    val rating = totalRating.sum.toDouble / snapshots.size
    new TeamSnapshot(team, snapshots, rating.toInt, Stats.empty)
  }

  def apply(team: Team, characterLadder: CharacterLadder): TeamSnapshot = {
    val snapshots = team.members.map(t => characterLadder.rows(t))
    TeamSnapshot(snapshots)
  }
}

final case class Team(members: Set[CharacterId])
