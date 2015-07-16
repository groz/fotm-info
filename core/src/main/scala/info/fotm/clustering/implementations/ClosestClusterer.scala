package info.fotm.clustering.implementations

import info.fotm.clustering.Clusterer
import info.fotm.util.MathVector

class ClosestClusterer extends Clusterer {

  def clusterize(input: Seq[MathVector], size: Int): Set[Seq[MathVector]] = {
    // take cluster, find vector closest to it, repeat
    val seedResult = Set[Seq[MathVector]]()
    val seedCluster = Seq[MathVector]()

    (0 to input.length).foldLeft((seedResult, seedCluster, input)) { (acc, i) =>
      val (result, cluster, left) = acc

      cluster.size match {
        case 0 =>
          (result, Seq(left.head), left.tail)
        case x: Int if x == size =>
          if (left.size > 0)
            (result + cluster, Seq(left.head), left.tail)
          else // last one
            (result + cluster, Seq(), left)
        case _ =>
          val avg = MathVector.avg(cluster)
          if (left.size != 0) {
            val nearest = left.minBy(_.distTo(avg))
            (result, cluster :+ nearest, left diff Seq(nearest))
          } else {
            (result, cluster, left)
          }
      }
    }._1
  }
}
