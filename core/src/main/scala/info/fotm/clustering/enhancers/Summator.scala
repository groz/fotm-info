package info.fotm.clustering.enhancers

import info.fotm.clustering.RealClusterer
import info.fotm.util.MathVector

class Summator(clusterers: RealClusterer*) extends RealClusterer {

  override def clusterize[T](input: Map[T, MathVector], groupSize: Int): Set[Seq[T]] =
      clusterers.map(_.clusterize(input, groupSize)).flatten.toSet

}
