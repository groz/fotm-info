package info.fotm.clustering.implementations.RMClustering

import info.fotm.clustering.Clusterer
import info.fotm.clustering.Clusterer._
import info.fotm.util.MathVector

class KMeansDiv2Clusterer(
    selectMaxCluster: (Set[Cluster] => Cluster) = _.filter(_.length > 1).maxBy(ClusterRoutines.meanDistToClosest)
  ) extends Clusterer {

  private val kmeans = new KMeansClusterer

  override def clusterize(input: Cluster, groupsCount: Int): Set[Cluster] = {
    require(groupsCount >= 1)
    clusterizationStep(Set(input), groupsCount)
  }

  def clusterize(initClusters: Set[Cluster], groupsCount: Int): Set[Cluster] = {
    require(groupsCount >= 1)
    clusterizationStep(initClusters, groupsCount)
  }

  /*
    Iteratively splits the "max" cluster into 2 until total number of clusters is bigger than 'count
  */
  def clusterizationStep(clusters: Set[Cluster], maxCount: Int, nTries: Int = 0, nMaxTries: Int = 100): Set[Cluster] = {
    require(maxCount >= 1)
    require(clusters.nonEmpty)

    if (maxCount == 1 || clusters.size >= maxCount || nTries > nMaxTries)
      clusters
    else {
      val maxCluster = selectMaxCluster(clusters)
      val newClusters = (clusters - maxCluster) ++ split(maxCluster)
      clusterizationStep(newClusters, maxCount, nTries + 1, nMaxTries)
    }
  }

  // splits cluster into 2 with k-means seeded by most distant points
  def split(input: Seq[MathVector]): Set[Cluster] = {

    if (input.isEmpty) Set()
    else if (input.size == 1) Set(input)
    else {
      val distances = for {
        x <- input
        y <- input
        if x != y
        dist = x distTo y
      } yield (x, y, dist)

      val (a, b, maxDist) = distances.maxBy(_._3)

      if (maxDist <= 1e-6) {
        // all points collide. just split them into 2 clusters
        val (left, right) = input.splitAt(input.size / 2)
        Set(left, right)
      } else
        kmeans.process(Set(input), Seq(a, b))
    }
  }
}
