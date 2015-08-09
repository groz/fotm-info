package info.fotm.clustering.implementations.RMClustering

import info.fotm.clustering.Clusterer
import info.fotm.clustering.Clusterer.Cluster
import info.fotm.util.{Rng, RandomNumberGenerator, Statistics, MathVector}

// https://en.wikipedia.org/wiki/K-means_clustering
class KMeansClusterer(rng: RandomNumberGenerator) extends Clusterer {
  def this() = this(new Rng())

  def clusterize(input: Seq[MathVector], nClusters: Int): Set[Cluster] = {
    require(nClusters >= 1)

    if (input.isEmpty)
      Set.empty
    else if (nClusters == 1)
      Set(input)
    else {
      val means = initialize_plusplus(input, nClusters)
      process(Set(input), means)
    }
  }

  def process(clusters: Set[Cluster], centroids: Seq[MathVector]): Set[Cluster] = {
    val newClusters: Set[Cluster] = assign(clusters.flatten.toSeq, centroids)

    if (newClusters != clusters) {
      val newCentroids = calcCentroids(newClusters)
      process(newClusters, newCentroids)
    }
    else
      newClusters
  }

  def initialize_plusplus(input: Cluster, nClusters: Int): Seq[MathVector] = {
    val firstCenterIndex = rng.nextInt(input.length)
    val centers = input(firstCenterIndex) :: Nil
    findCenters(input diff centers, centers, nClusters)
  }

  def findCenters(input: Cluster, centers: List[MathVector], nCenters: Int): Seq[MathVector] = {
    if (centers.length < nCenters && input.nonEmpty) {
      val inputDistancesToCenters = input.map(_ distTo centers)
      val newCenter = Statistics.randomWeightedValue(input, inputDistancesToCenters, rng.nextDouble())
      findCenters(input diff Seq(newCenter), newCenter :: centers, nCenters)
    }
    else
      centers
  }

  def assign(input: Seq[MathVector], centroids: Seq[MathVector]): Set[Cluster] =
    input.groupBy(v => centroids.minBy(_ distTo v)).values.toSet

  def calcCentroids(clusters: Set[Cluster]): Seq[MathVector] = clusters.map(MathVector.avg).toSeq
}
