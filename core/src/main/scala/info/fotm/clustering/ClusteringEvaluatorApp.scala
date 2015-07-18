package info.fotm.clustering

import info.fotm.clustering.ClusteringEvaluatorData.DataPoint
import info.fotm.clustering.FeatureSettings.features
import info.fotm.clustering.enhancers._
import info.fotm.clustering.implementations._
import com.github.nscala_time.time.Imports._

object ClusteringEvaluatorApp extends App {
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
      "HT3" -> RealClusterer.wrap(new HTClusterer3)
    )

    for ((name, clusterer) <- clusterers) {
      val startTime = DateTime.now.toInstant

      val result = evaluator.evaluate(clusterer, data)

      val elapsed = new Period(startTime, DateTime.now)
      println(s"$name = $result, time: $elapsed")
    }
  }
}
