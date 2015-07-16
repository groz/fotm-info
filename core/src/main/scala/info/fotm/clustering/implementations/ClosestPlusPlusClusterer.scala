package info.fotm.clustering.implementations

import info.fotm.clustering.Clusterer
import info.fotm.clustering.Clusterer.Cluster
import info.fotm.util.MathVector

class ClosestPlusPlusClusterer extends Clusterer {

  def init(input: Cluster, groupSize: Int): (Seq[Cluster], Cluster) = {
    val nClusters: Int = input.size / groupSize

    // take the point furthest from all other clusters
    (1 to nClusters).foldLeft((Seq.empty[Seq[MathVector]], input)) { (acc,  i) =>
      val (clusters, left: Cluster) = acc

      if (clusters.isEmpty)
        (clusters :+ Seq(left.head), left diff Seq(left.head))
      else {
        val furthest: MathVector = left.maxBy(v => clusters.flatten.map(_.distTo(v)).sum)
        (clusters :+ Seq(furthest), left diff Seq(furthest))
      }
    }
  }

  override def clusterize(input: Cluster, groupSize: Int): Set[Cluster] = {
    val (seedClusters: Seq[Cluster], vectorsLeft) = init(input, groupSize)

    // add points to nearest clusters that are not yet full
    vectorsLeft.foldLeft(seedClusters) { (clusters, v) =>
      val nonFullClusters = clusters.filter(_.size < groupSize)

      val closestCluster = nonFullClusters.minBy { cluster =>
        cluster.map(_.distTo(v)).sum
      }

      val newCluster = closestCluster :+ v
      (clusters diff Seq(closestCluster)) :+ newCluster
    }.toSet
  }
}
