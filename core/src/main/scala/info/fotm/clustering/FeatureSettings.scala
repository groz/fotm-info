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
  val startingWeights = MathVector(2.939211277186743,1.4575688290092734,1.1576878743787917,0.24236125545624632,-0.04996126301468273,-0.05175639160257861,-0.049961263014681734,-0.049866782562688616,-0.04986678256268817,-0.052228793862549416,-0.04996126301468218,-0.049961263014681956,1.6555998563897192,-0.05015022391866997,1.8976587743995852,1.173844031669843,0.21165510855803826,-0.049866782562689504)
  lazy val features = Feature.reweigh(defaultFeatures.zip(startingWeights.coords)).toList
}
