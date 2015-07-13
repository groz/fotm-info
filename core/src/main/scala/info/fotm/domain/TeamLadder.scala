package info.fotm.domain

final case class TeamLadder(axis: Axis, rows: Map[Team, TeamSnapshot])
