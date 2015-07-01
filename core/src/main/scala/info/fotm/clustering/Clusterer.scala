package info.fotm.clustering

import info.fotm.util.MathVector

import scala.util.Random

trait Clusterer {
  type Cluster = Seq[MathVector]
  def clusterize(input: Cluster, groupSize: Int): Set[Cluster]
}

class RandomClusterer extends Clusterer {
  val rng = new Random()

  override def clusterize(input: Cluster, groupSize: Int): Set[Cluster] =
    rng.shuffle(input).sliding(groupSize, groupSize).toSet
}