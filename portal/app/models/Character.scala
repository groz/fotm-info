package models

case class Region(code: String)

case class Realm(name: String, slug: String)(implicit val region: Region)

case class CharacterClass(id: Int, spec: Option[Int])

case class Stats(seasonWins: Int, seasonLosses: Int, weeklyWins: Int, weeklyLosses: Int) {
  val seasonTotal = seasonWins + seasonLosses
  val weeklyTotal = weeklyWins + weeklyLosses
  lazy val seasonWinRatio = if (seasonTotal == 0) None else Some(seasonWins.toDouble / seasonTotal)
  lazy val weeklyWinRatio = if (weeklyTotal == 0) None else Some(weeklyWins.toDouble / weeklyTotal)
}

case class Character(name: String, charClass: CharacterClass, realm: Realm, charStats: Stats)

abstract case class Team(characters: List[Character], teamStats: Stats)(implicit val bracket: Bracket) {
  def copy(characters: List[Character] = this.characters, teamStats: Stats = this.teamStats) = Team(characters, teamStats)
}

object Team {
  def apply(characters: List[Character], teamStats: Stats)(implicit bracket: Bracket) = {
    val orderedChars = characters.sortBy(c => (c.realm.slug, c.name))
    new Team(orderedChars.toList, teamStats) {}
  }
}

case class Setup(classes: List[CharacterClass])(implicit val bracket: Bracket)