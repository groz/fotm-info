package info.fotm.clustering.implementations.RMClustering

import info.fotm.clustering.Clusterer
import info.fotm.clustering.Clusterer._
import ClusterRoutines._

class RMClusterer extends Clusterer {

  def clusterize(input: Cluster, groupSize: Int): Set[Cluster] = {

    val initClusterization = makeInitialClusterization(input, groupSize)
    val oneMerged = onePointMerge(initClusterization)
    val merged = merge(oneMerged)

    val kDiv2 = new KMeansDiv2Clusterer(cs => cs.maxBy(_.length))
    val divClusters = kDiv2.clusterize(merged, initClusterization.size)

    val eqClusters = movePoint(divClusters.filter(_.nonEmpty).toList, groupSize).toSet

    val tuned = new FineTuner(groupSize).fineTuning(eqClusters)

    try {
      new FineTuner(groupSize).fineTuning2(tuned).filter(_.length == groupSize)
    } catch {
      case _: Throwable => tuned
    }
  }

  // moves one point from one cluster to another
  def movePoint(clusters: List[Cluster], groupSize: Int): List[Cluster] = {

    val graph: Graph[Int] = makeGraphFromClusters(clusters, groupSize)

    val negativeVertices = getNegativeVertices(graph)

    if (negativeVertices.isEmpty)
      clusters
    else {
      val positiveVertices = getPositiveVertices(graph)

      if (positiveVertices.isEmpty)
        clusters
      else {
        val dijkstraAlg = new DijkstraAlg(graph)
        val paths = dijkstraAlg.findShortestPathsFrom(positiveVertices)
        val optPath = findOptimalPath(graph, paths)
        val newClusters = movePointAlongPath(clusters, optPath)
        movePoint(newClusters, groupSize)
      }
    }
  }
}
