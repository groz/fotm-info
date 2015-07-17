package info.fotm.clustering

import info.fotm.clustering.ClusteringEvaluatorData.DataPoint
import info.fotm.clustering.implementations.RMClustering.EqClusterer2
import info.fotm.clustering.implementations.{HTClusterer3, ClosestClusterer}
import info.fotm.util.MathVector

object FeatureEvaluatorApp extends App {

  val settings = EvaluatorSettings(
    matchesPerTurn = 20,
    ladderSize = 5000,
    teamSize = 3,
    hopRatio = 0.05,
    turnsPerWeek = 600)

  val dataGen: ClusteringEvaluatorData = new ClusteringEvaluatorData(settings)
  val start = settings.turnsPerWeek * 4 / 3
  val end = start + 2 * settings.turnsPerWeek
  val data: Stream[DataPoint] = dataGen.updatesStream().slice(start, end)

  val (prevLadder, lastladder, _) = data.last
  lastladder.rows.toList.sortBy(-_._2.stats.rating).map(_._2.stats).foreach(println)

  def estimate(fs: Seq[Feature[CharacterStatsUpdate]]): Double = {
    val clusterer = RealClusterer.wrap(new ClosestClusterer())
    //val clusterer = RealClusterer.wrap(new HTClusterer3)
    //val clusterer = RealClusterer.wrap(new HTClusterer3(Some(new EqClusterer2)))
    val evaluator = new ClusteringEvaluator(fs.toList)
    1 - evaluator.evaluate(clusterer, data)
  }

//  val startingWeights = MathVector(2.179498600996277,1.593301777294845,1.817542615061698,0.27151402114766976,0.7440438353774109,0.7438512713247858,-0.041006394687579384,0.763906482541476,0.01250563169272434,0.798455058753051,0.005920247667347223,0.8278914956535278,0.2491892886456094,0.8060805544258177,0.0797461392031601,1.571142710682229,0.247217196929698,0.7530892028608863)
//  val weightedFeatures = Feature.reweigh(ClusteringEvaluatorApp.features.zip(startingWeights.coords))
//  val weights = ML.findWeights(weightedFeatures, estimate)

  val weights = ML.findWeights(ClusteringEvaluatorApp.features, estimate)
  println(weights)
}
