package models

import info.fotm.domain.CharacterSnapshot

object AltText {

  val factionMap = Map(
    0 -> "Alliance",
    1 -> "Horde"
  )

  val genderMap = Map(
    0 -> "Male",
    1 -> "Female"
  )

  val raceMap = Map(
  )

  def raceGender(char: CharacterSnapshot) = {
  }
}
