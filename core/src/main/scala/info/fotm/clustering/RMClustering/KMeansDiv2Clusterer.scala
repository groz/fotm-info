package info.fotm.clustering.RMClustering

import info.fotm.clustering.Clusterer
import info.fotm.clustering.Clusterer._
import info.fotm.util.MathVector


class KMeansDiv2Clusterer(maxClusterCond: Set[Cluster] => Cluster = cs => cs.filter(c => c.length > 1).maxBy(ClusterRoutines.meanDistToClosest)) extends Clusterer
{

  private val kmeans = new KMeansClusterer

  override def clusterize(input: Cluster, groupsCount: Int): Set[Cluster] =
  {
    if (groupsCount == 1)
      Set(input)
    else
      clusterizationStep(Set(input), groupsCount)
  }

  def clusterize(initClusters: Set[Cluster], groupsCount: Int): Set[Cluster] =
  {
    if (initClusters.size == groupsCount)
      initClusters
    else
      clusterizationStep(initClusters, groupsCount)
  }

  // TODO: fix StackOverflow workaround here
  def clusterizationStep(clusters: Set[Cluster], clustersCount: Int, i: Int = 0): Set[Cluster] =
  {
    val maxCluster = maxClusterCond(clusters)
    val newClusters = (clusters - maxCluster) ++ divide(maxCluster)
    if (i > 100 || newClusters.size >= clustersCount)
      newClusters
    else
      clusterizationStep(newClusters, clustersCount, i + 1)
  }

  // divides cluster in two parts
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

    kmeans.clusterize(cluster, 2, Seq(x, y))
  }

  def dist(x: MathVector, y: MathVector): Double =
  {
    x.distTo1(y)
  }

}
