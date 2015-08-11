package info.fotm.clustering.implementations

import info.fotm.clustering.Clusterer
import info.fotm.clustering.Clusterer.Cluster
import info.fotm.util.{Statistics, MathVector}

import scala.collection.mutable

/*
Hasan-Timur clusterer (minor mod+refactoring by Tagir)
 */
class HTClusterer(leftoverClusterer: Option[Clusterer] = None) extends Clusterer {
  // TODO: write tests for all separate methods

  def distTo(v: MathVector, cluster: Seq[MathVector]): Double = cluster.view.map(_.distTo(v)).min

  def fastLinearization(as: mutable.ListBuffer[(MathVector, Double)], bs: mutable.Set[MathVector]): Seq[(MathVector, Double)] = {
    while(bs.nonEmpty) {
      var (nearestB, dist) = (bs.head, Double.MaxValue)
      for (b <- bs) {
        val distToA = as.view.map(_._1.distTo(b)).min
        if (distToA < dist) {
          dist = distToA
          nearestB = b
        }
      }
      as += ((nearestB, dist))
      bs -= nearestB
    }
    as.seq
  }

  def clusterLinearization(as: Seq[(MathVector, Double)], bs: Seq[MathVector]): Seq[(MathVector, Double)] =
    if (bs.isEmpty) as
    else {
      val aVectors = as.map(_._1)
      val distancesToAs = bs.view.map(b => (b, distTo(b, aVectors)))
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
    //val linearized: Seq[(MathVector, Double)] = clusterLinearization(Seq((input.head, 0.0)), input.tail)
    val linearized = fastLinearization(mutable.ListBuffer((input.head, 0.0)), mutable.Set[MathVector](input.tail: _*))

    val distances: Seq[Double] = linearized.map(_._2)
    val maxDistance: Double = distances.max
    val avgDistance: Double = distances.sum / linearized.length
    val meanDistance: Double = Statistics.mean(distances)

    val nSteps = 10
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
      val correctlySized: Set[Cluster] = clusters.filter(_.size == groupSize)

      leftoverClusterer.fold(correctlySized) { clusterer =>
        val rest = input diff correctlySized.flatten.toSeq
        correctlySized ++ clusterer.clusterize(rest, groupSize)
      }
    } else {
      Set(input)
    }
  }
}
