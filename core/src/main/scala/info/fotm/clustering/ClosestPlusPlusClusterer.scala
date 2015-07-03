package info.fotm.clustering

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
        val averages: Seq[MathVector] = clusters.map(MathVector.avg)
        val furthest: MathVector = left.maxBy(v => averages.map(_.distTo(v)).sum)
        (clusters :+ Seq(furthest), left diff Seq(furthest))
      }
    }
  }

  override def clusterize(input: Cluster, groupSize: Int): Set[Cluster] = {
    val (seedClusters: Seq[Cluster], vectorsLeft) = init(input, groupSize)

    // add points to nearest clusters that are not yet full
    vectorsLeft.foldLeft(seedClusters) { (clusters, v) =>
      val closestCluster = clusters.filter(_.size < groupSize).minBy(MathVector.avg(_).distTo(v))
      val newCluster = closestCluster :+ v
      (clusters diff Seq(closestCluster)) :+ newCluster
    }.toSet
  }
}