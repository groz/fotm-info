package info.fotm.domain

final case class TeamSnapshot /* private */ (team: Team, view: TeamView, stats: Stats) {
  lazy val rating = view.rating
  lazy val factionId = view.snapshots.head.view.factionId
}

object TeamSnapshot {
  def fromSnapshots(snapshots: Set[CharacterSnapshot]): TeamSnapshot = {
    val team = Team(snapshots.map(_.id))
    new TeamSnapshot(team, TeamView(snapshots), Stats.empty)
  }

  def fromLadder(team: Team, characterLadder: CharacterLadder): TeamSnapshot = {
    val snapshots = team.members.map(t => characterLadder.rows(t))
    TeamSnapshot.fromSnapshots(snapshots)
  }
}

final case class Team(members: Set[CharacterId])

final case class TeamView(snapshots: Set[CharacterSnapshot]) {
  lazy val rating = {
    val totalRating = snapshots.toList.map(_.stats.rating)
    totalRating.sum.toDouble / snapshots.size
  }

  lazy val sortedSnapshots: Seq[CharacterSnapshot] =
    snapshots.toSeq.sorted(CharacterOrderingFactory.ordering)
}


object CharacterOrderingFactory {
  val healers = Set(65, 105, 256, 257, 264, 270)
  val melee = Set(66, 70, 71, 72, 73, 103, 104, 250, 251, 252, 259, 260, 261, 263, 268, 269)

  def cmpValue(cs: CharacterSnapshot) = (
    if (healers.contains(cs.view.specId)) 1 else 0,
    if (melee.contains(cs.view.specId)) 0 else 1,
    cs.id.classId,
    cs.view.specId
  )

  val ordering: Ordering[CharacterSnapshot] = Ordering.by(cmpValue)
}
