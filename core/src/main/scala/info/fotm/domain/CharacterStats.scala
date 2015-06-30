package info.fotm.domain

case class CharacterStats(
                          id: CharacterId,
                          rating: Int,
                          weeklyWins: Int,
                          weeklyLosses: Int,
                          seasonWins: Int,
                          seasonLosses: Int)
