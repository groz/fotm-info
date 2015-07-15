package info.fotm.crawler

import info.fotm.domain.{CharacterLadder, CharacterId}

case class LadderUpdate(previous: CharacterLadder, current: CharacterLadder) {

  lazy val commonIds: Set[CharacterId] = previous.rows.keySet.intersect(current.rows.keySet)

  lazy val distances: Set[Int] = commonIds.map(id => current(id).season.total - previous(id).season.total)

  lazy val distance: Int = distances.map(Math.abs).max
}
