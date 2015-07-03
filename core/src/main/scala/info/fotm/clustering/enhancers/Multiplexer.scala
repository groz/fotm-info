package info.fotm.clustering.enhancers

import info.fotm.clustering.Clusterer
import info.fotm.clustering.Clusterer.Cluster

import scala.collection.immutable.IndexedSeq
import scala.util.Random

trait Multiplexer extends Clusterer {
  lazy val rng = new Random
  lazy val nTimes = 20
  lazy val threshold = 4

  abstract override def clusterize(input: Cluster, groupSize: Int): Set[Cluster] = {
    val allClusters = for {
      i <- 0 to nTimes
      cluster <- super.clusterize(rng.shuffle(input), groupSize)
    } yield cluster

    val groups = allClusters.groupBy(identity)
    groups.filter(_._2.size >= threshold).keySet // take those found at least threshold times
  }
}
