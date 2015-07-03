package info.fotm.clustering.enhancers

import info.fotm.clustering.{RealClusterer, Clusterer}
import info.fotm.clustering.Clusterer.Cluster
import info.fotm.util.MathVector

import scala.collection.immutable.{TreeMap, IndexedSeq}
import scala.util.Random

trait Multiplexer extends RealClusterer {
  protected lazy val multiplexRng = new Random
  protected lazy val multiplexTurns = 20
  protected lazy val multiplexThreshold = 2

  abstract override def clusterize[T](input: Map[T, MathVector], groupSize: Int): Set[Seq[T]] = {
    class RandomOrder extends Ordering[T] {
      lazy val shuffled: List[(T, MathVector)] = multiplexRng.shuffle(input.toList)
      lazy val lookup: Map[T, Int] = shuffled.zipWithIndex.map { kvi =>
        val ((k, v), i) = kvi
        (k, i)
      }.toMap

      def compare(t1: T, t2: T): Int = lookup(t1).compareTo(lookup(t2))
    }

    val allClusters = for {
      i <- 0 to multiplexTurns
      randomInput = TreeMap.empty[T, MathVector](new RandomOrder) ++ input
      cluster <- super.clusterize(randomInput, groupSize)
    } yield cluster

    val groups = allClusters.groupBy(identity)
    groups.filter(_._2.size >= multiplexThreshold).keySet // take those found at least threshold times
  }
}
