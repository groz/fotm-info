package info.fotm.domain

final case class TeamSnapshot private (team: Team, view: TeamView, stats: Stats) {
  lazy val rating = view.rating
}

object TeamSnapshot {
  def apply(snapshots: Set[CharacterSnapshot]): TeamSnapshot = {
    val team = Team(snapshots.map(_.id))
    new TeamSnapshot(team, TeamView(snapshots), Stats.empty)
  }

  def apply(team: Team, characterLadder: CharacterLadder): TeamSnapshot = {
    val snapshots = team.members.map(t => characterLadder.rows(t))
    TeamSnapshot(snapshots)
  }
}

final case class Team(members: Set[CharacterId])

final case class TeamView(snapshots: Set[CharacterSnapshot]) {
  lazy val rating = {
    val totalRating = snapshots.toList.map(_.stats.rating)
    totalRating.sum.toDouble / snapshots.size
  }
}
