package info.fotm.clustering

import info.fotm.util.MathVector

class ClosestClusterer extends Clusterer {

  def clusterize(input: Seq[MathVector], size: Int): Set[Seq[MathVector]] = {
    // take cluster, find vector closest to it, repeat
    val seedResult = Set[Seq[MathVector]]()
    val seedCluster = Seq[MathVector]()

    (0 to input.length).foldLeft((seedResult, seedCluster, input)) { (acc, i) =>
      val (result, cluster, left) = acc

      if (cluster.size == 0) { // first cluster
        (result, Seq(left.head), left.tail)
      } else if (cluster.size == size) { // full cluster
        if (left.size > 0)
          (result + cluster, Seq(left.head), left.tail)
        else
          (result + cluster, Seq(), left)
      } else { // grow cluster
        val avg = MathVector.avg(cluster)
        val nearest = left.minBy(_.distTo(avg))
        (result, cluster :+ nearest, left diff Seq(nearest))
      }
    }._1
  }
}