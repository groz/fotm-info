package info.fotm.domain

case class CharacterInfo(
                          id: CharacterId,
                          rating: Int,
                          weeklyWins: Int,
                          weeklyLosses: Int,
                          seasonWins: Int,
                          seasonLosses: Int)
