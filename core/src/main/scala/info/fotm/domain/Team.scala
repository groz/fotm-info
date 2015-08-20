package info.fotm.domain

final case class TeamSnapshot /* private */ (team: Team, view: TeamView, stats: Stats) {
  lazy val rating = view.rating
  lazy val factionId = view.snapshots.head.view.factionId
}

object TeamSnapshot {
  def fromView(teamView: TeamView): TeamSnapshot = fromSnapshots(teamView.snapshots)

  def fromUpdate(teamUpdate: TeamUpdate): TeamSnapshot = {
    val initStats = if (teamUpdate.won) Stats.empty.win else Stats.empty.loss
    fromView(teamUpdate.view).copy(stats = initStats)
  }

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
  lazy val teamId = Team(snapshots.map(_.id))

  lazy val rating = {
    val totalRating = snapshots.toList.map(_.stats.rating)
    totalRating.sum.toDouble / snapshots.size
  }

  lazy val sortedSnapshots: Seq[CharacterSnapshot] =
    snapshots.toSeq.sorted(CharacterOrderingFactory.snapshotsBySpecOrdering)
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

  def cmpBySpecId(specId: Int) = (
    if (healers.contains(specId)) 1 else 0,
    if (melee.contains(specId)) 0 else 1,
    specId
  )

  val snapshotsBySpecOrdering: Ordering[CharacterSnapshot] = Ordering.by(cmpValue)

  def specIdOrdering[T](getSpecId: T => Int): Ordering[T] = Ordering.by(t => cmpBySpecId(getSpecId(t)))
}
