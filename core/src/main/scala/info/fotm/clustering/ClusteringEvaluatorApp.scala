package info.fotm.clustering

import info.fotm.clustering.ClusteringEvaluatorData.DataPoint
import info.fotm.clustering.enhancers._
import info.fotm.clustering.implementations.RMClustering.EqClusterer2
import info.fotm.clustering.implementations._

object ClusteringEvaluatorApp extends App {
  def sqr(x: Double) = x * x

  lazy val features = List[Feature[CharacterStatsUpdate]](
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

  val evaluator = new ClusteringEvaluator(features)

  for ((_, settings) <- Defaults.settings) {
    println(s"Evaluating $settings:")
    val dataGen: ClusteringEvaluatorData = new ClusteringEvaluatorData(settings)
    val data: Stream[DataPoint] = dataGen.updatesStream().slice(500, 700)

    def createCMV(turns: Int, threshold: Int): (String, RealClusterer) = {
      s"C * M($turns, $threshold) * V" ->
        new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer with Verifier {
          override lazy val multiplexTurns = turns
          override lazy val multiplexThreshold = threshold
        }
    }

    val clusterers: Map[String, RealClusterer] = Map(
      //        "Random" -> RealClusterer.wrap(new RandomClusterer),
      "HT2" -> RealClusterer.wrap(new HTClusterer2),
      "HT3" -> RealClusterer.wrap(new HTClusterer3),
      "HT2 * V" -> new ClonedClusterer(RealClusterer.wrap(new HTClusterer2)) with Verifier,
      "HT3 * V" -> new ClonedClusterer(RealClusterer.wrap(new HTClusterer3)) with Verifier,
      "HT3[RM]" -> RealClusterer.wrap(new HTClusterer3(Some(new EqClusterer2))),
      "HT3[RM] * V" -> new ClonedClusterer(RealClusterer.wrap(new HTClusterer3(Some(new EqClusterer2)))) with Verifier,
      "RM" -> RealClusterer.wrap(new EqClusterer2),
      "RM * V" -> new ClonedClusterer(RealClusterer.wrap(new EqClusterer2)) with Verifier,
      createCMV(20, 3),
      "(HT3 + CM) * V" -> new Summator(
        RealClusterer.wrap(new HTClusterer3),
        new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer
      ) with Verifier,
      "(HT3[RM] + CM) * V" -> new Summator(
        RealClusterer.wrap(new HTClusterer3(Some(new EqClusterer2))),
        new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer
      ) with Verifier
    )

    for ((name, clusterer) <- clusterers.par) {
      val result = evaluator.evaluate(clusterer, data)
      println(s"$name = $result")
    }
  }
}
