package info.fotm.clustering

import info.fotm.clustering.Clusterer.Cluster
import info.fotm.util.MathVector

/*
Hasan-Timur clusterer (minor mod+refactoring by Tagir)
 */
class HTClusterer3(addition: Option[Clusterer] = None) extends Clusterer {
  // TODO: write tests for all separate methods

  def distTo(v: MathVector, cluster: Seq[MathVector]): Double = cluster.map(_.distTo(v)).min

  def clusterLinearization(as: Seq[(MathVector, Double)], bs: Seq[MathVector]): Seq[(MathVector, Double)] =
    if (bs.isEmpty) as
    else {
      val distancesToAs: Seq[(MathVector, Double)] = bs.map(b => (b, distTo(b, as.map(_._1))))
      val (nearestB, dist) = distancesToAs.minBy(_._2)
      clusterLinearization(as :+(nearestB, dist), bs diff Seq(nearestB))
    }

  def findClusters(linearizedInput: Seq[(MathVector, Double)], maxDistance: Double): Set[Cluster] = {
    val (lastCluster, result) = linearizedInput.foldLeft(Seq.empty[MathVector], Set.empty[Cluster]) { (acc, input) =>
      val (currentCluster, result) = acc
      val (vector, distance) = input

      if (distance < maxDistance) {
        (currentCluster :+ vector, result)
      } else {
        val r = if (currentCluster.nonEmpty) result + currentCluster else result
        (Seq(vector), r)
      }
    }

    result + lastCluster
  }

  def clusterize(input: Cluster, groupSize: Int): Set[Cluster] = {
    val linearized: Seq[(MathVector, Double)] = clusterLinearization(Seq((input.head, 0.0)), input.tail)

    val distances: Seq[Double] = linearized.map(_._2)
    val maxDistance: Double = distances.max
    val avgDistance: Double = distances.sum / linearized.length
    val meanDistance: Double = distances.sorted.apply(distances.size / 2)

    val nSteps = 100
    val stepSize = maxDistance / nSteps

    def searching(distance: Double, maxDistance: Double, result: Set[Cluster]): Set[Cluster] = {
      if (distance >= maxDistance) result
      else {
        val clusters = findClusters(linearized, distance)
        searching(distance + stepSize, maxDistance, result ++ clusters)
      }
    }

    if (maxDistance != 0) {
      val clusters = searching(meanDistance, maxDistance, Set())
      val correctlySized = clusters.filter(_.size == groupSize)
      val rest = input diff correctlySized.flatten.toSeq

      if (rest.isEmpty || addition.isEmpty) correctlySized
      else {
        val additionalClusterer = addition.get
        val restClusters = additionalClusterer.clusterize(rest, groupSize)
        correctlySized ++ restClusters
      }
    } else {
      Set(input)
    }
  }
}
