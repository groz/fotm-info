package info.fotm.clustering

import info.fotm.domain.CharacterStats

case class CharFeatures(
                         prevInfo: CharacterStats,
                         nextInfo: CharacterStats) {
  require(prevInfo.id == nextInfo.id)
}
