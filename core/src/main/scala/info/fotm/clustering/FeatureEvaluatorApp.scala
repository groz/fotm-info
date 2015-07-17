package info.fotm.clustering

import info.fotm.clustering.ClusteringEvaluatorData.DataPoint
import info.fotm.clustering.implementations.ClosestClusterer
import info.fotm.util.MathVector

object FeatureEvaluatorApp extends App {

  val dataGen: ClusteringEvaluatorData = new ClusteringEvaluatorData()
  val data: Stream[DataPoint] = dataGen.updatesStream().slice(500, 650)

  def estimate(fs: Seq[Feature[CharacterStatsUpdate]]): Double = {
    val clusterer = RealClusterer.wrap(new ClosestClusterer())  //RealClusterer.wrap(new HTClusterer3)
    val evaluator = new ClusteringEvaluator(fs.toList)
    1 - evaluator.evaluate(clusterer, data)
  }

  //val startingWeights = MathVector(1.920017017655817,1.6852265475430763,1.7828121676239097,0.36236970857264583,-0.01680493512018555,0.019357583492875974,-0.051903850244627514,-0.026909168262069905,0.015369070410552244,0.01563497128270752,0.03584343756647701)

  val startingWeights = MathVector(1.5682110507723457,1.3505010228909948,1.4818716676603696,0.8184576198032117,0.9422036862186365,0.8817479865781163,0.0772021568910849,0.9480543351087883,0.5557253802836957,0.9283748797510101,0.4945504555038086,0.9601092121126161,0.847000689133825,0.9503571250713407,0.7927522342800631,1.33808954999056,0.7838856036341458,0.9377733875687569)

  val weightedFeatures = Feature.reweigh(ClusteringEvaluatorApp.features.zip(startingWeights.coords))
  val weights = ML.findWeights(weightedFeatures, estimate)

  //val weights = ML.findWeights(ClusteringEvaluatorApp.features, estimate)
  println(weights)
}
