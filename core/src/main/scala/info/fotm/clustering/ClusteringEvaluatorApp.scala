package info.fotm.clustering

import info.fotm.clustering.ClusteringEvaluatorData.DataPoint
import info.fotm.clustering.FeatureSettings.features
import info.fotm.clustering.enhancers._
import info.fotm.clustering.implementations.RMClustering.EqClusterer2
import info.fotm.clustering.implementations._

object ClusteringEvaluatorApp extends App {
  val evaluator = new ClusteringEvaluator(features)

  for ((_, settings) <- Defaults.settings) {
    println(s"Evaluating $settings:")
    val dataGen: ClusteringEvaluatorData = new ClusteringEvaluatorData(settings)
    val data: Stream[DataPoint] = dataGen.updatesStream().slice(settings.startTurn, settings.endTurn)

    def createCMV(turns: Int, threshold: Int): (String, RealClusterer) = {
      s"C * M($turns, $threshold) * V" ->
        new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer with Verifier {
          override lazy val multiplexTurns = turns
          override lazy val multiplexThreshold = threshold
        }
    }

    val clusterers: Map[String, RealClusterer] = Map(
      //"Random" -> RealClusterer.wrap(new RandomClusterer),
      //"Closest" -> RealClusterer.wrap(new ClosestClusterer)
      //"HT3" -> RealClusterer.wrap(new HTClusterer3)
      "HT3[RM]" -> RealClusterer.wrap(new HTClusterer3(Some(new EqClusterer2)))
      //"RM" -> RealClusterer.wrap(new EqClusterer2)
//      "HT3 * V" -> new ClonedClusterer(RealClusterer.wrap(new HTClusterer3)) with Verifier,
//      "HT3[RM] * V" -> new ClonedClusterer(RealClusterer.wrap(new HTClusterer3(Some(new EqClusterer2)))) with Verifier,
//      "RM * V" -> new ClonedClusterer(RealClusterer.wrap(new EqClusterer2)) with Verifier,
//      createCMV(20, 3),
//      "(HT3 + CM) * V" -> new Summator(
//        RealClusterer.wrap(new HTClusterer3),
//        new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer
//      ) with Verifier,
//      "(HT3[RM] + CM) * V" -> new Summator(
//        RealClusterer.wrap(new HTClusterer3(Some(new EqClusterer2))),
//        new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer
//      ) with Verifier
    )

    for ((name, clusterer) <- clusterers) {
      val result = evaluator.evaluate(clusterer, data)
      println(s"$name = $result")
    }
  }
}
