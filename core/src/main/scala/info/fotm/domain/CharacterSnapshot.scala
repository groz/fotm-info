package info.fotm.domain

import info.fotm.api.models.LeaderboardRow
import info.fotm.domain.TeamSnapshot.SetupFilter

final case class CharacterSnapshot(id: CharacterId, view: CharacterView, stats: CharacterStats) {
  def this(raw: LeaderboardRow) = this(
    id = CharacterId(
      name = raw.name,
      realmName = raw.realmName,
      realmId = raw.realmId,
      realmSlug = raw.realmSlug,
      classId = raw.classId),

    view = CharacterView(
      specId = raw.specId,
      genderId = raw.genderId,
      raceId = raw.raceId,
      factionId = raw.factionId),

    stats = CharacterStats(
      rating = raw.rating,
      weekly = Stats(raw.weeklyWins, raw.weeklyLosses),
      season = Stats(raw.seasonWins, raw.seasonLosses)
    )
  )

  def matchesFilter(filter: SetupFilter) =
    if (filter.isEmpty)
      true
    else
      filter.exists{ f =>
        val (classId: Int, specId: Option[Int]) = f
        id.classId == classId && specId.map(_ == view.specId).getOrElse(true)
      }
}

object CharacterSnapshot {
  def fromRaw(raw: LeaderboardRow) = new CharacterSnapshot(raw)
}


final case class CharacterId(name: String, realmName: String, realmId: Int, realmSlug: String, classId: Int)

final case class CharacterView(specId: Int, genderId: Int, raceId: Int, factionId: Int)

final case class CharacterStats(rating: Int, weekly: Stats, season: Stats) {
  def update(diff: Int): CharacterStats =
    CharacterStats(rating + diff,
      if (diff > 0) weekly.win else weekly.loss,
      if (diff > 0) season.win else season.loss)
}
