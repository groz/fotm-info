package info.fotm.clustering.enhancers

import info.fotm.clustering.{RealClusterer, Clusterer}
import info.fotm.clustering.Clusterer.Cluster
import info.fotm.util.MathVector
import scala.collection.mutable

// WARNING: makes Clusterer mutable, use with caution

trait Verifier extends RealClusterer {
  protected lazy val verifierThreshold = 2
  val seen = new mutable.HashMap[Seq[Any], Int]()

  abstract override def clusterize[T](input: Map[T, MathVector], groupSize: Int): Set[Seq[T]] = {
    val clusters = super.clusterize(input, groupSize).map(c => c.sortBy(_.hashCode))
    clusters.foreach(c => seen.update(c, 1 + seen.getOrElse(c, 0)))
    clusters.filter(seen(_) >= verifierThreshold)
  }
}

