package info.fotm.clustering.implementations

import info.fotm.clustering.Clusterer

import scala.util.Random

class RandomClusterer extends Clusterer {
  import Clusterer._
  val rng = new Random()

  override def clusterize(input: Cluster, groupSize: Int): Set[Cluster] =
    rng.shuffle(input).grouped(groupSize).toSet
}
