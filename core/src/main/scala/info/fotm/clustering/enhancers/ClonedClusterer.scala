package info.fotm.clustering.enhancers

import info.fotm.clustering.RealClusterer
import info.fotm.util.MathVector

class ClonedClusterer(from: RealClusterer) extends RealClusterer {
  override def clusterize[T](input: Map[T, MathVector], groupSize: Int): Set[Seq[T]] =
    from.clusterize(input, groupSize)
}
