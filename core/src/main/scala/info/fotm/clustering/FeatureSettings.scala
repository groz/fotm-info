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

  //lazy val startingWeights = MathVector(defaultFeatures.map(_.weight): _*)

  lazy val startingWeights =
    MathVector(
      12.852989130434821,   // rating
      5.968931159420276,    // ratingDerivative
      4.141576086956533,    // ratingDiff
      3.0,                  // weeklyWinsRatio
      0.0,                  // seasonWinsRatio
      3.0,                  // weeklyTotal
      0.0,                  // seasonTotal
      2.0,                  // weeklyWins
      0.0,                  // seasonWins
      2.0,                  // weeklyLosses
      0.0,                  // seasonLosses
      // ========================
      3.0,                  // weeklyDiff
      1.523460144927535,    // seasonDiff
      3.0,                  // weeklyDiffSqr
      1.936684782608684,    // seasonDiffSqr
      8.47282608695654,     // ratingDiffSqr
      0.0,                  // seasonWinsRatioSqr
      1.0)                  // weeklyWinsRatioSqr

    //MathVector(10.0, 4.947290554297682, 3.9245008509864188, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5.617074058685279, 0, 6.438833041471343, 3.979327535042576, 0, 0)
    //MathVector(2.8128878640744457,2.365736698172473,2.1150888266780514,1.7327148005022326,0.3537944136973483,0.8186993678222529,0.0,0.6894010972052322,0.0,1.1751600226144494,0.0,0.9090362094228353,2.4500330593300763,0.0,1.140232696173764,1.2359704158062994,1.411336217018037,0.838994409813651)

  lazy val features = Feature.reweigh(defaultFeatures.zip(startingWeights.coords)).toList
}
