package info.fotm.domain

import info.fotm.api.models.Leaderboard
import scala.collection.breakOut
import scala.collection.immutable.IndexedSeq

final case class CharacterLadder(axis: Axis, rows: Map[CharacterId, CharacterSnapshot])
  extends (CharacterId => CharacterStats) {

  override def apply(id: CharacterId): CharacterStats = rows(id).stats

  def calcTeamRating(team: Team): Double = {
    var totalRating = 0.0
    for (m <- team.members) {
      totalRating += rows(m).stats.rating
    }
    totalRating / team.members.size
  }
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
