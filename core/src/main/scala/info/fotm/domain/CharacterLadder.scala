package info.fotm.domain

import info.fotm.api.models.Leaderboard

final case class CharacterLadder(axis: Axis, rows: Map[CharacterId, CharacterSnapshot])
  extends (CharacterId => CharacterStats) {

  override def apply(id: CharacterId): CharacterStats = rows(id).stats
}

object CharacterLadder {
  def apply(axis: Axis, raw: Leaderboard): CharacterLadder = {
    val rows = for {
      row <- raw.rows
      charInfo = CharacterSnapshot(row)
    } yield (charInfo.id, charInfo)
    CharacterLadder(axis, rows.toMap)
  }
}
