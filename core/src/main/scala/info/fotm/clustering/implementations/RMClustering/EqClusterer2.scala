package info.fotm.clustering.implementations.RMClustering

import info.fotm.clustering.Clusterer
import info.fotm.clustering.Clusterer._
import ClusterRoutines._


class EqClusterer2 extends Clusterer
{
  //type MathCluster = Seq[MathVector]

  //def toMathCluster(cluster: Cluster): MathCluster = cluster.map(c => MathVector(c))

  //def toCluster(mathCluster: MathCluster): Cluster = mathCluster.map(c => c.coords)

  def clusterize(input: Cluster, groupSize: Int): Set[Cluster] =
  {
    // moves one point from one cluster to another
    def movePoint(clusters: List[Cluster]): List[Cluster] =
    {
      val graph = makeGraphFromClusters(clusters, groupSize)
      val negativeVertices = getNegativeVertices(graph)
      if (negativeVertices.isEmpty)
        clusters
      else
      {
        val positiveVertices = getPositiveVertices(graph)
        val dijkstraAlg = new DijkstraAlg(graph)
        val paths = dijkstraAlg.findShortestPathsFrom(positiveVertices)
        val optPath = findOptimalPath(graph, paths)
        val newClusters = movePointAlongPath(clusters, optPath)
        movePoint(newClusters)
      }
    }

//    if (input.map(x => input.count(y => x.coords == y.coords)).exists(x => x>1))
//      println(input)
//
//    import EqClusterer._
//    num = num + 1
//    if (num==306)
//      FileWriter(input.toString)
//    if (num<306)
//      return Set(input)
//    println(num)

    val initClusterization = makeInitialClusterization(input, groupSize)
    val oneMerged = onePointMerge(initClusterization)
    val merged = merge(oneMerged)

    val kDiv2 = new KMeansDiv2Clusterer(cs => cs.maxBy(_.length))
    val divClusters = kDiv2.clusterize(merged, initClusterization.size)

    val eqClusters = movePoint(divClusters.toList).toSet

    val tuned = new FineTuner(groupSize).fineTuning(eqClusters)

    try
    {
      new FineTuner(groupSize).fineTuning2(tuned).filter(_.length == groupSize)
    }
    catch
    {
      case _: Throwable => tuned
    }

    //val n = uneqClusters.map(x => difference(groupSize, x.length)).sum //negative clusters deviation sum
    //val p = uneqClusters.map(x => difference(x.length, groupSize)).sum //positive clusters deviation sum
    //val k = math.min(n, p) //they're equal for now
    //val sortedpaths = DijkstraAlg.findAllFromPositiveShortestPaths(uneqClusters).sortWith(pathesComparer)
    //val eqClusters = sortedpaths.scanLeft(uneqClusters)((acc, xpath) => passMaxByPath(acc, xpath, groupSize)).last
    //eqClusters.toSet
  }



}
