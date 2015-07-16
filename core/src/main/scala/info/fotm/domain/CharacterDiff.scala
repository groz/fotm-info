package info.fotm.domain

final case class CharacterDiff(previous: CharacterSnapshot, current: CharacterSnapshot) {
  require(previous.id == current.id)
  val id = current.id
  val view = current.view
  val stats = current.stats
}
