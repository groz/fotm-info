package info.fotm.clustering

import info.fotm.util.MathVector

/*
Hasan-Timur clusterer
 */
class HTClusterer extends Clusterer {
  def clusterize(input: Cluster, groupSize: Int): Set[Cluster] = {
    def clusterLinearization (a: Seq[(MathVector, Double)], b: Cluster) : Seq[(MathVector, Double)] =
      if (b.isEmpty) a else {
        val t = b.map(x => (x, a.minBy(y => y._1.distTo(x))._1)).minBy(z => z._1.distTo(z._2))
        val e = (t._1, t._1.distTo(t._2))
        val na = a :+ e
        val nb = b.filter(x => x != e._1)
        clusterLinearization(na, nb)
      }

    val l = clusterLinearization(Seq((input.head, 0.0)), input.tail)

    val max = l.map(x => x._2).max
    val avg = l.map(x => x._2).sum / l.length

    val tol = avg * 4

    def findClusters (l: Seq[(MathVector, Double)], tol: Double) : Set[Cluster] = {
      def iter(l: Seq[(MathVector, Double)], current: Cluster, result: Set[Cluster]): Set[Cluster] = {
        if (l.isEmpty) result + current else if (l.head._2 < tol) iter(l.tail, current :+ l.head._1, result) else {
          val r = if (!current.isEmpty) result + current else result
          val c = Seq(l.head._1)
          iter(l.tail, c, r)
        }
      }

      iter(l, Seq(), Set())
    }

    //findClusters(l, tol)

    val p = 20
    var h = max / p
    def searching(tol: Double, maxTol: Double, acc: Set[Cluster]): Set[Cluster] = {
      if (tol > maxTol) acc else searching(tol + h, maxTol, acc ++ findClusters(l, tol).filter(x => x.size == groupSize))
    }

    searching(avg, max, Set())
  }
}