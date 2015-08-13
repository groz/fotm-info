package info.fotm.clustering

import info.fotm.clustering.Clusterer.Cluster
import info.fotm.clustering.enhancers.ClonedClusterer
import info.fotm.util.MathVector

object Clusterer {
  type V = MathVector
  type Cluster = Seq[MathVector]
}

trait Clusterer {
  def clusterize(input: Cluster, groupSize: Int): Set[Cluster]
  def toReal = RealClusterer.wrap(this)
  def toRealSeen = new ClonedClusterer(this.toReal) with SeenEnhancer
  /*
   TODO:
     change to ``def clusterize[T](input: Map[T, MathVector], groupSize: Int): Set[Seq[T]]''
     instead of using RealClusterer
  */
}
