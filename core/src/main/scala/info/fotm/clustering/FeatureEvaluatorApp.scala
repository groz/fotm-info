package info.fotm.clustering

import info.fotm.clustering.ClusteringEvaluatorData.DataPoint
import info.fotm.clustering.FeatureSettings._
import info.fotm.clustering.enhancers.{SimpleMultiplexer, Verifier, ClonedClusterer}
import info.fotm.clustering.implementations.{HTClusterer, ClosestClusterer}
import info.fotm.domain.LadderUpdate

object FeatureEvaluatorApp extends App {

  val settings = EvaluatorSettings()
  val dataGen: ClusteringEvaluatorData = new ClusteringEvaluatorData(settings)
  val data: Stream[DataPoint] = dataGen.updatesStream().slice(settings.startTurn, settings.endTurn)

  // uncomment following lines for viewing ladder state
//  val (ladderUpdate: LadderUpdate, games) = data.last
//  ladderUpdate.current.rows.toList.sortBy(-_._2.stats.rating).map(_._2.stats).foreach(println)

  def estimate(fs: Seq[Feature[CharacterStatsUpdate]]): Double = {
//    val clusterer = RealClusterer.sequence(
//      new HTClusterer().toReal,
//      new SimpleMultiplexer(new ClosestClusterer, 10, 3).toReal
//    )
    val clusterer = new ClosestClusterer().toReal
    val evaluator = new ClusteringEvaluator(fs.toList)
    1 - evaluator.evaluate(clusterer, data)
  }

  val weights = ML.findWeights(features, estimate)
  println(weights)
}
