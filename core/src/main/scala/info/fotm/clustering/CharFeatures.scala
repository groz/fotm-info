package info.fotm.clustering

import info.fotm.domain.{CharacterId, CharacterStats}

final case class CharFeatures(id: CharacterId, prev: CharacterStats, next: CharacterStats) {
  lazy val won: Boolean = prev.season.wins < next.season.wins
  lazy val lost: Boolean = !won
}
