package info.fotm.domain

object Domain {
  type LadderSnapshot = Map[CharacterId, CharacterStats]
  type Game = (Team, Team)
}
