package info.fotm.domain

final case class LadderDiff(charDiffs: Set[CharacterDiff]) {
  def splitW(set: Set[CharacterDiff]): (Set[CharacterDiff], Set[CharacterDiff]) =
    set.partition(c => c.stats.season.wins > c.previous.stats.season.wins)

  lazy val (horde, alliance) = charDiffs.partition(_.view.factionId == 0)
  lazy val (winners, losers) = splitW(charDiffs)
  lazy val (hordeWinners, hordeLosers) = splitW(horde)
  lazy val (allianceWinners, allianceLosers) = splitW(alliance)
}
