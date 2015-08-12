package info.fotm.clustering

import java.util.UUID

import info.fotm.clustering.Clusterer.Cluster
import info.fotm.util.MathVector
import scala.collection.breakOut

object RealClusterer {

  // TODO: works only as long as MathVector has referential equality
  def wrap(clusterer: Clusterer) = new RealClusterer {

    override def clusterize[T](input: Map[T, MathVector], groupSize: Int): Set[Seq[T]] = {
      require(groupSize > 1)

      if (input.size < groupSize)
        Set.empty
      else if (input.size == groupSize)
        Set(input.keys.toSeq)
      else if (groupSize == 1)
        input.keys.map(Seq(_)).toSet
      else {
        val reverseMap = input.map(_.swap)
        val clusters: Set[Cluster] = clusterer.clusterize(input.values.toSeq, groupSize)

        clusters
          .filter(_.size == groupSize)  // additional protection against misbehaving clusterers
          .map(vectors => vectors.map(reverseMap))
      }
    }
  }

  lazy val identity = new RealClusterer {
    override def clusterize[T](input: Map[T, MathVector], groupSize: Int): Set[Seq[T]] = Set(input.keys.toSeq)
  }

}

trait RealClusterer {
  def clusterize[T](input: Map[T, MathVector], groupSize: Int): Set[Seq[T]]

  // def clusterize(input: Seq[MathVector], groupSize: Int): Seq[Int]
}


trait SeenEnhancer extends RealClusterer {
  /*
    - remember id of latest clusterize call for each input item
    - ignore items without previous id data
    - group all others per their latest update id, i.e. who they were seen with previously
    - find updates inside each groups separately and merge them
  */
  val seen = scala.collection.mutable.HashMap.empty[Any, UUID]

  abstract override def clusterize[T](input: Map[T, MathVector], groupSize: Int): Set[Seq[T]] = {

    val inputSeenPreviously: Map[T, MathVector] = for {
      (k, v) <- input
      if seen.contains(k)
    } yield (k, v)

    val updateGroups: Map[UUID, Map[T, MathVector]] = inputSeenPreviously.groupBy(kv => seen(kv._1))

    val result: Set[Seq[T]] = updateGroups.flatMap(g => super.clusterize(g._2, groupSize))(breakOut)

    val updateId: UUID = java.util.UUID.randomUUID()
    for ((k, v) <- input) {
      seen(k) = updateId
    }

    result
  }
}
