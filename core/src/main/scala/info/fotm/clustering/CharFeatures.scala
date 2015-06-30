package info.fotm.clustering

import info.fotm.domain.CharacterInfo

case class CharFeatures(
                         prevInfo: CharacterInfo,
                         nextInfo: CharacterInfo) {
  require(prevInfo.id == nextInfo.id)
}
