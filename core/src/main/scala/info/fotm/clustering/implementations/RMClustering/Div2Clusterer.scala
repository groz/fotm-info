package info.fotm.clustering.implementations.RMClustering

import info.fotm.clustering.Clusterer
import info.fotm.clustering.Clusterer.Cluster
import info.fotm.util.MathVector

/**
 * Created by Admin on 04.07.2015.
 */
class Div2Clusterer extends Clusterer
{
  private val kmeans = new KMeansClusterer

  override def clusterize(input: Cluster, groupsCount: Int): Set[Cluster] =
  {
    process(Set(input), groupsCount)
  }

  def process(clusters: Set[Cluster], clustersCount: Int): Set[Cluster] =
  {
    val maxCluster = clusters.maxBy(c => c.length)
    val newClusters = (clusters - maxCluster) ++ divide(maxCluster)
    if (newClusters.size == clustersCount)
      newClusters
    else
      process(newClusters, clustersCount)
  }

  def divide(cluster: Cluster): Set[Cluster] =
  {
    // find max distant points
    val pairs =
      for
      {
        x <- cluster
        y <- cluster
      } yield (x, y)
    val (x, y) = pairs.maxBy(p => dist(p._1, p._2))

    // divide by closeness to x or y
    val (c1, c2) = cluster.partition(z => dist(z, x) < dist(z, y))
    Set(c1, c2)
  }

  def dist(x: MathVector, y: MathVector): Double =
  {
    x.distTo1(y)
  }

}
