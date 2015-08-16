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
      "CsM(20,10)" -> (() => new SimpleMultiplexer(new ClosestClusterer, 20, 10).toReal),
      "CsM(10,4)" -> (() => new SimpleMultiplexer(new ClosestClusterer, 10, 4).toReal),
      "CsM(10,2)" -> (() => new SimpleMultiplexer(new ClosestClusterer, 10, 2).toReal),
      "HT" -> (() => new HTClusterer().toReal),
      "HT -> HT" -> (() => RealClusterer.sequence(
        new HTClusterer().toReal,
        new HTClusterer().toReal
      )),
      "HT -> CsM(10, 3)" -> (() => RealClusterer.sequence(
        new HTClusterer().toReal,
        new SimpleMultiplexer(new ClosestClusterer, 10, 3).toReal
      )),
      "(20, 10) -> (10, 4) -> (10, 2)" -> (() => RealClusterer.sequence(
        new SimpleMultiplexer(new ClosestClusterer, 20, 10).toReal,
        new SimpleMultiplexer(new ClosestClusterer, 10, 4).toReal,
        new SimpleMultiplexer(new ClosestClusterer, 10, 2).toReal
      ))
    )

    for ((name, clustererBuilder) <- clusterers.par) {
      val result = evaluator.evaluate(clustererBuilder(), data)
      println(s"$name = $result")
    }
  }
}
