package info.fotm.clustering

import info.fotm.clustering.ClusteringEvaluatorData.DataPoint
import info.fotm.clustering.implementations.RMClustering.EqClusterer2
import info.fotm.clustering.implementations.{HTClusterer3, ClosestClusterer}
import info.fotm.util.MathVector
import info.fotm.clustering.FeatureSettings._

object FeatureEvaluatorApp extends App {

  val settings = EvaluatorSettings(
    matchesPerTurn = 20,
    ladderSize = 5000,
    teamSize = 3,
    hopRatio = 0.05,
    turnsPerWeek = 200)

  val dataGen: ClusteringEvaluatorData = new ClusteringEvaluatorData(settings)
  val start = settings.turnsPerWeek * 4 / 3
  val end = start + 300 //2 * settings.turnsPerWeek
  val data: Stream[DataPoint] = dataGen.updatesStream().slice(start, end)

  // uncomment following line for viewing ladder state
  //val (prevLadder, lastladder, _) = data.last
  //lastladder.rows.toList.sortBy(-_._2.stats.rating).map(_._2.stats).foreach(println)

  def estimate(fs: Seq[Feature[CharacterStatsUpdate]]): Double = {
    val clusterer = RealClusterer.wrap(new ClosestClusterer())
    //val clusterer = RealClusterer.wrap(new HTClusterer3)
    //val clusterer = RealClusterer.wrap(new HTClusterer3(Some(new EqClusterer2)))
    val evaluator = new ClusteringEvaluator(fs.toList)
    1 - evaluator.evaluate(clusterer, data)
  }

  val weights = ML.findWeights(features, estimate)
  println(weights)
}
