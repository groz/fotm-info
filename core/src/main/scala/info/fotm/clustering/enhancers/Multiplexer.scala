package info.fotm.clustering.enhancers

import info.fotm.clustering.{RealClusterer, Clusterer}
import info.fotm.clustering.Clusterer.Cluster
import info.fotm.util.MathVector

import scala.collection.immutable.{TreeMap, IndexedSeq}
import scala.util.Random

trait Multiplexer extends RealClusterer {
  protected lazy val multiplexRng = new Random
  protected lazy val multiplexTurns = 20
  protected lazy val multiplexThreshold = 3

  abstract override def clusterize[T](input: Map[T, MathVector], groupSize: Int): Set[Seq[T]] = {
    class RandomOrder extends Ordering[T] {
      lazy val shuffled: List[(T, MathVector)] = multiplexRng.shuffle(input.toList)
      lazy val lookup: Map[T, Int] = shuffled.zipWithIndex.map { kvi =>
        val ((k, v), i) = kvi
        (k, i)
      }.toMap

      def compare(t1: T, t2: T): Int = lookup(t1).compareTo(lookup(t2))
    }

    val allClusters: IndexedSeq[Seq[T]] = for {
      i <- 0 to multiplexTurns
      randomInput = TreeMap.empty[T, MathVector](new RandomOrder) ++ input
      cluster <- super.clusterize(randomInput, groupSize)
    } yield cluster

    val groups = allClusters.groupBy(identity)
    groups.filter(_._2.size >= multiplexThreshold).keySet // take those found at least threshold times
  }
}

class SimpleMultiplexer(underlying: Clusterer, turns: Int, threshold: Int) extends Clusterer {
  protected lazy val multiplexRng = new Random
  protected lazy val multiplexTurns = turns
  protected lazy val multiplexThreshold = threshold

  override def clusterize(input: Cluster, groupSize: Int): Set[Cluster] = {

    val vectors: Map[MathVector, Int] = input.zipWithIndex.toMap

    val allClusters: IndexedSeq[Cluster] = for {
      i <- 1 to multiplexTurns
      randomInput = multiplexRng.shuffle(input)
      cluster <- underlying.clusterize(randomInput, groupSize)
    } yield cluster

    val groups = allClusters.groupBy(_.map(vectors).sorted)

    groups
      .filter(kv => kv._2.size >= multiplexThreshold) // take those found at least threshold times
      .map(_._2.head)
      .filter(_.size == groupSize)
      .toSet
  }
}
