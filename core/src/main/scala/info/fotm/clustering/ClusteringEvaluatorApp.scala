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

    def createCMV(turns: Int, threshold: Int) = {
      new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer with Verifier {
        override lazy val multiplexTurns = turns
        override lazy val multiplexThreshold = threshold
      }
    }

    val clusterers: Map[String, () => RealClusterer] = Map(
      //"Random" -> RealClusterer.wrap(new RandomClusterer),
      //      "Closest" -> (() => RealClusterer.wrap(new ClosestClusterer)),
      //"HT3[RM]" -> (() => RealClusterer.wrap(new HTClusterer(Some(new RMClusterer)))),
      //      "Closest x Seen" -> (() => new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with SeenEnhancer),
      //"HT3[RM] x Seen" -> (() => new ClonedClusterer(RealClusterer.wrap(new HTClusterer3(Some(new EqClusterer2)))) with SeenEnhancer),
      "RM" -> (() => RealClusterer.wrap(new RMClusterer)),
      "RM x Seen" -> (() => new ClonedClusterer(RealClusterer.wrap(new RMClusterer)) with SeenEnhancer)
//      "CxV(20, 3)" -> (() => createCMV(20, 3)),
//      "CxSeen(20, 3)" -> (() =>
//        new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer with SeenEnhancer {
//          override lazy val multiplexTurns = 20
//          override lazy val multiplexThreshold = 3
//        }
//        ),
//      "C(20, 3)" -> (() =>
//        new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer {
//          override lazy val multiplexTurns = 20
//          override lazy val multiplexThreshold = 3
//        }
//        )
    )

    for ((name, clustererBuilder) <- clusterers) {
      val result = evaluator.evaluate(clustererBuilder(), data)
      println(s"$name = $result")
    }
  }
}
