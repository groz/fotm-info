package info.fotm.clustering

import scala.util.Random

class RandomClusterer extends Clusterer {
  import Clusterer._
  val rng = new Random()

  override def clusterize(input: Cluster, groupSize: Int): Set[Cluster] =
    rng.shuffle(input).sliding(groupSize, groupSize).toSet
}
