package info.fotm.clustering

import info.fotm.util.MathVector

object FeatureSettings {
  def sqr(x: Double) = x * x

  lazy val defaultFeatures = List[Feature[CharacterStatsUpdate]](
    Feature[CharacterStatsUpdate]("rating",           u => u.next.rating),

    Feature[CharacterStatsUpdate]("ratingDerivative", u => sqr(u.next.rating - u.prev.rating) / u.prev.rating.toDouble),
    Feature[CharacterStatsUpdate]("ratingDiff",       u => Math.abs(u.next.rating - u.prev.rating)),

    Feature[CharacterStatsUpdate]("seasonWinsRatio",  u => u.next.season.wins / u.next.season.total.toDouble),
    Feature[CharacterStatsUpdate]("weeklyWinsRatio",  u => u.next.weekly.wins / u.next.weekly.total.toDouble),

    Feature[CharacterStatsUpdate]("weeklyTotal",      u => u.next.weekly.total),
    Feature[CharacterStatsUpdate]("seasonTotal",      u => u.next.season.total),

    Feature[CharacterStatsUpdate]("weeklyWins",       u => u.next.weekly.wins),
    Feature[CharacterStatsUpdate]("seasonWins",       u => u.next.season.wins),

    Feature[CharacterStatsUpdate]("weeklyLosses",     u => u.next.weekly.losses),
    Feature[CharacterStatsUpdate]("seasonLosses",     u => u.next.season.losses),

    // ==============
    Feature[CharacterStatsUpdate]("weeklyDiff",       u => u.next.weekly.wins - u.next.weekly.losses),
    Feature[CharacterStatsUpdate]("seasonDiff",       u => u.next.season.wins - u.next.season.losses),

    Feature[CharacterStatsUpdate]("weeklyDiffSqr",    u => sqr(u.next.weekly.wins - u.next.weekly.losses)),
    Feature[CharacterStatsUpdate]("seasonDiffSqr",    u => sqr(u.next.season.wins - u.next.season.losses)),

    Feature[CharacterStatsUpdate]("ratingDiffSqr",       u => sqr(u.next.rating - u.prev.rating)),
    Feature[CharacterStatsUpdate]("seasonWinsRatioSqr",  u => sqr(u.next.season.wins / u.next.season.total.toDouble)),
    Feature[CharacterStatsUpdate]("weeklyWinsRatioSqr",  u => sqr(u.next.weekly.wins / u.next.weekly.total.toDouble))

    //    Feature.const[CharacterStatsUpdate]
  )

  lazy val startingWeights = MathVector(defaultFeatures.map(_.weight): _*)

  //val startingWeights = MathVector(10.0,4.947290554297682,3.9245008509864188,0,0,0,0,0,0,0,0,0,5.617074058685279,0,6.438833041471343,3.979327535042576,0,0)

  lazy val features = Feature.reweigh(defaultFeatures.zip(startingWeights.coords)).toList
}
