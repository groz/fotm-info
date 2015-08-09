package info.fotm.clustering.implementations.RMClustering

import info.fotm.clustering.Clusterer._
import info.fotm.util.MathVector

import scala.collection.immutable.IndexedSeq

object ClusterRoutines {
  def merge(clusters: Set[Cluster], num: Int = 0, prevDev: Double = Double.MaxValue): Set[Cluster] = {
    if (clusters.size == 1)
      clusters
    else {
      val deviations: Map[Cluster, Double] = clusters.map(c => c -> meanAbsDeviation(c)).toMap
      // max iterations is limited to clusters count
      if (num > 2 * clusters.size || (deviations.values.sum - prevDev).abs < 0.000000001) {
        if (num > 2 * clusters.size)
          println("Merge limit exceeded")
        clusters
      }
      else {
        val pairs = cartesianProd(clusters, clusters).filter(p => p._1 != p._2)
        val (c1, c2) = pairs.minBy(p => distance(p._1, p._2) - deviations(p._1))
        if (distance(c1, c2) < 1.05 * deviations(c1)) {
          // TODO: add check if the clusters are the same before and after mergeAndDivide
          val mergingClusters = Set(c1, c2)
          val newClusters = mergeAndDivide(mergingClusters)
          merge((clusters -- mergingClusters) ++ newClusters, num + 1, deviations.values.sum)
        }
        else
          clusters
      }
    }
  }

  def mergeAndDivide(clusters: Set[Cluster]): Set[Cluster] = {
    val count = clusters.size
    val clusterer = new KMeansDiv2Clusterer
    clusterer.clusterize(clusters.toSeq.flatMap(c => c), count)
  }

  def onePointMerge(clusters: Set[Cluster]): Set[Cluster] = {
    val oneClusterOpt = clusters.find(c => c.length == 1)
    if (!oneClusterOpt.isDefined)
      clusters
    else {
      val oneCluster = oneClusterOpt.get
      val other = clusters - oneCluster
      val target = other.minBy(distance(oneCluster, _))
      onePointMerge((other - target) + (target ++ oneCluster))
    }
  }

  def makeInitialClusterization(input: Cluster, groupSize: Int): Set[Cluster] = {
    val approxCountOfGroups = input.length / groupSize
    val kmeansClusterer = new KMeansDiv2Clusterer
    val clusterizations = (1 to 2).map(j => kmeansClusterer.clusterize(input, approxCountOfGroups))
    // TODO: remove this workaround against StackOverflow
    if (clusterizations.isEmpty)
      Set()
    else {
      val bySize: Map[Int, IndexedSeq[Set[Cluster]]] = clusterizations.groupBy(c => Math.abs(c.size - approxCountOfGroups))
      val closestClusterings = bySize(bySize.keys.min)
      closestClusterings.minBy(estimateClusterization)
    }
  }

  def makeGraphFromClusters(clusters: List[Cluster], groupSize: Int): Graph[Int] = {
    if (clusters.exists(c => c.isEmpty))
      throw new IllegalArgumentException("Clusters can't contain empty one")

    val distances = clusters.map(x => clusters.map(y => distance(x, y)).toVector).toVector
    val labels = clusters.indices.map(i => (i, clusters(i).length - groupSize)).toMap
    new Graph[Int](distances, labels)
  }

  def getPositiveVertices(graph: Graph[Int]): Set[Int] = {
    //graph.labels.filterKeys(x => x > 0).values.toSet
    graph.labels.filter(x => x._2 > 0).keys.toSet
  }

  def getNegativeVertices(graph: Graph[Int]): Set[Int] = {
    //graph.labels.filterKeys(x => x > 0).values.toSet
    graph.labels.filter(x => x._2 < 0).keys.toSet
  }

  def findOptimalPath(graph: Graph[Int], paths: Set[GraphPath]): GraphPath = {
    // filter paths that ended on negative vertices
    paths.filter(p => graph.labels(p.end) < 0)
      // and get optimal
      .minBy(x => (x.length, x.path.length))
  }

  def movePointAlongPath(clusters: List[Cluster], graphPath: GraphPath): List[Cluster] = {
    val pathOfClusters = graphPath.path.map(i => clusters(i)).toList
    val pathOfClustersRenewed = passByPath(pathOfClusters)

    // removes clusters that are part of path, because they were changed
    //clusters.diff(pathOfClusters).union(pathOfClustersRenewed)
    clusters.zipWithIndex
      .filterNot(c => graphPath.path.contains(c._2))
      .map(c => c._1) ::: pathOfClustersRenewed
  }


  def estimateClusterization(clusters: Set[Cluster]): Double = {
    val listOfClusters = clusters.toList
    val centers = listOfClusters.map(getClusterGeometricCenter)
    listOfClusters.zip(centers).foldLeft(0.0)((acc, x) => acc + meanAbsDeviation(x._1, x._2))
  }

  def meanAbsDeviation(cluster: Cluster, fromVector: V): Double = {
    cluster.foldLeft(0.0)((acc, v) => acc + distance(v, fromVector)) / cluster.length
  }

  def meanAbsDeviation(cluster: Cluster): Double = {
    meanAbsDeviation(cluster, getClusterGeometricCenter(cluster))
  }

  def meanDistToClosest(cluster: Cluster): Double =
    cluster.map(x => distance(x, cluster diff Seq(x))).sum / cluster.size

  /*=========================================================================*/

  def passByPath(pathOfClusters: List[Cluster]): List[Cluster] = {
    val (first, otherClusters) = pathOfClusters.splitAt(1)
    otherClusters.foldLeft(first)((acc, x) => passLastElement(acc, x))
  }

  def passLastElement(clusters: List[Cluster], next: Cluster): List[Cluster] = {
    val passed = passElement(clusters.last, next)
    clusters.init ::: passed
  }

  def passElement(c1: Cluster, c2: Cluster): List[Cluster] = {
    val passVec = closestVector(c1, c2)
    List(c1.filter(c => c != passVec), passVec :: c2.toList)
  }

  /* ================================================ */
  def cartesianProd[TX, TY](colX: Iterable[TX], colY: Iterable[TY]): Iterable[(TX, TY)] = {
    for {
      x <- colX
      y <- colY
    } yield (x, y)
  }

  def difference(a: Int, b: Int): Int = {
    if (a < b) 0 else a - b
  }

  def pathesComparer(graph: Graph[Int], path1: GraphPath, path2: GraphPath): Boolean = {
    if (path1.maxRoute(graph) != path2.maxRoute(graph)) path1.maxRoute(graph) < path2.maxRoute(graph)
    else path1.length < path2.length
  }


  /*=========================================================================*/

  def closestVector(v: V, to: Cluster): V = {
    to.minBy(x => distance(x, v))
  }

  //closest to cluster 'to' vector from cluster 'from'
  def closestVector(from: Cluster, to: Cluster): V = {
    from.minBy(x => distance(x, to.minBy(y => distance(x, y))))
  }

  def closestVectors(c1: Cluster, c2: Cluster): Seq[V] = {
    c1.sortBy(x => distance(x, c2.minBy(y => distance(x, y))))
  }

  def distance(v: V, cluster: Cluster): Double = {
    cluster.map(x => distance(x, v)).min
  }

  def distance(c1: Cluster, c2: Cluster): Double = {
    distance(c1.minBy(x => distance(x, c2)), c2)
  }

  def getClusterGeometricCenter(cl: Cluster): V = {
    MathVector.avg(cl)
  }

  def getClusterCenter(cl: Cluster): V = {
    closestVector(getClusterGeometricCenter(cl), cl)
  }

  def distanceBetweenCenters(c1: Cluster, c2: Cluster): Double = {
    distance(getClusterCenter(c1), getClusterCenter(c2))
  }

  def distance(a: V, b: V): Double = a distTo1 b
}
