package info.fotm.clustering

import info.fotm.clustering.ClusteringEvaluatorData.DataPoint
import info.fotm.clustering.FeatureSettings.features
import info.fotm.clustering.enhancers._
import info.fotm.clustering.implementations.RMClustering.RMClusterer
import info.fotm.clustering.implementations._

object ClusteringEvaluatorApp extends App {
  val evaluator = new ClusteringEvaluator(features)

  for ((_, settings) <- Defaults.settings) {
    println(s"Evaluating $settings:")
    val dataGen: ClusteringEvaluatorData = new ClusteringEvaluatorData(settings)
    val data: Stream[DataPoint] = dataGen.updatesStream().slice(settings.startTurn, settings.endTurn)

    val clusterers: Map[String, () => RealClusterer] = Map(
      //"Random" -> RealClusterer.wrap(new RandomClusterer),
      "Closest" -> (() => new ClosestClusterer().toReal),
      "CsM(10,3)" -> (() => new SimpleMultiplexer(new ClosestClusterer, 10, 3).toReal),
      "CsM(20,3)" -> (() => new SimpleMultiplexer(new ClosestClusterer, 20, 3).toReal),
      //"RM" -> (() => new RMClusterer().toReal),
      "HT3" -> (() => new HTClusterer().toReal),
      "HT3[RM]" -> (() => new HTClusterer(Some(new RMClusterer)).toReal),
      "HT3[CsM(10,2)]" -> (() => new HTClusterer(Some(new SimpleMultiplexer(new ClosestClusterer, 10, 2))).toReal)
    )

    for ((name, clustererBuilder) <- clusterers) {
      val result = evaluator.evaluate(clustererBuilder(), data)
      println(s"$name = $result")
    }
  }
}
