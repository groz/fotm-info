package info.fotm.domain

import info.fotm.domain.Domain.LadderSnapshot

case class Team(members: Set[CharacterId]) {
  def rating(ladder: LadderSnapshot): Int = {
    val charInfos: Set[CharacterStats] = members.map(ladder)
    val totalRating = charInfos.toSeq.map(_.rating).sum
    val result = totalRating / members.size.toDouble
    result.toInt
  }
}
