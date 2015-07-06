package info.fotm.clustering

import info.fotm.domain.CharacterStats

case class CharFeatures(
                         prevInfo: CharacterStats,
                         nextInfo: CharacterStats) {
  require(prevInfo.id == nextInfo.id)

  val id = nextInfo.id
  lazy val won: Boolean = prevInfo.seasonWins < nextInfo.seasonWins
  lazy val lost: Boolean = !won
}
