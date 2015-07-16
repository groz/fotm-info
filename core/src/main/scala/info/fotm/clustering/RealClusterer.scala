package info.fotm.clustering

import info.fotm.clustering.Clusterer.Cluster
import info.fotm.util.MathVector

object RealClusterer {

  // TODO: works only as long as MathVector has referential equality
  def wrap(clusterer: Clusterer) = new RealClusterer {

    override def clusterize[T](input: Map[T, MathVector], groupSize: Int): Set[Seq[T]] = {
//      println(s"Started ${clusterer.getClass}.clusterize")

      val result: Set[Seq[T]] = if (input.size < groupSize)
        Set()
      else {
        val reverseMap = input.map(_.swap)
        val clusters: Set[Cluster] = clusterer.clusterize(input.values.toSeq, groupSize)
        clusters.map(vectors => vectors.map(reverseMap))
      }

//      println(s"Finished ${clusterer.getClass}.clusterize")

      result
    }

  }

}

trait RealClusterer {
  def clusterize[T](input: Map[T, MathVector], groupSize: Int): Set[Seq[T]]
  // def clusterize(input: Seq[MathVector], groupSize: Int): Seq[Int]
}
