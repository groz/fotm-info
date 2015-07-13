package info.fotm.domain

import info.fotm.api.models.LeaderboardRow

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
}

object CharacterSnapshot {
  def apply(raw: LeaderboardRow) = new CharacterSnapshot(raw)
}


final case class CharacterId(name: String, realmName: String, realmId: Int, realmSlug: String, classId: Int)

final case class CharacterView(specId: Int, genderId: Int, raceId: Int, factionId: Int)

final case class CharacterStats(rating: Int, weekly: Stats, season: Stats)
