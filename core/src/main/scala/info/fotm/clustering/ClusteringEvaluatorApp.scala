package info.fotm.clustering

import info.fotm.clustering.ClusteringEvaluatorData.DataPoint
import info.fotm.clustering.enhancers._
import info.fotm.clustering.implementations.RMClustering.EqClusterer2
import info.fotm.clustering.implementations._

object ClusteringEvaluatorApp extends App {
  def sqr(x: Double) = x * x

  val features = List[Feature[CharacterStatsUpdate]](
    Feature("rating",           u => u.next.rating),
    Feature("ratingDerivative", u => sqr(u.next.rating - u.prev.rating) / u.prev.rating.toDouble),
    Feature("ratingDiff",       u => Math.abs(u.next.rating - u.prev.rating)),
    Feature("seasonWinsRatio",  u => u.next.season.wins / u.next.season.total.toDouble),
    Feature("weeklyWinsRatio",  u => u.next.weekly.wins / u.next.weekly.total.toDouble),
    Feature("weeklyTotal",      u => u.next.weekly.total)
  )

  val evaluator = new ClusteringEvaluator(features)

  for ((_, settings) <- Defaults.settings) {
    println(s"Evaluating $settings:")
    val dataGen: ClusteringEvaluatorData = new ClusteringEvaluatorData(settings)

    // Runner
    val data: Stream[DataPoint] = dataGen.prepareData().slice(500, 700)
    //val (prevLadder, lastladder, _) = data.last
    //lastladder.values.toList.sortBy(-_.rating).map(i => (i.rating, i.seasonWins, i.seasonLosses, i.weeklyWins, i.weeklyLosses)).foreach(println)

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
