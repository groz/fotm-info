import info.fotm.clustering.Clusterer.Cluster
import info.fotm.clustering.implementations.RMClustering.KMeansClusterer
import info.fotm.util.{Rng, MathVector}
import org.scalatest.{Matchers, FlatSpec}

import scala.util.Random
import ComparerTestHelpers._

class KMeansClustererSpec extends FlatSpec with Matchers {

  val input = Seq(
    MathVector(0), MathVector(50), MathVector(100),
    MathVector(1), MathVector(51), MathVector(101),
    MathVector(2), MathVector(52), MathVector(102)
  )

  val output = Set(
    Seq(MathVector(50), MathVector(51), MathVector(52)),
    Seq(MathVector(0), MathVector(1), MathVector(2)),
    Seq(MathVector(100), MathVector(101), MathVector(102))
  )

  "calcCentroids" should "return empty set if there are no clusters" in {
    val kmeansClusterer = new KMeansClusterer
    val centroids: Seq[MathVector] = kmeansClusterer.calcCentroids(Set.empty)
    centroids.size should be(0)
  }

  it should "return single element for single element cluster" in {
    val kmeansClusterer = new KMeansClusterer
    val c = MathVector(1, 1)
    val centroids: Seq[MathVector] = kmeansClusterer.calcCentroids(Set(Seq(c)))
    centroids should contain theSameElementsInOrderAs Seq(c)
  }

  it should "correctly work for general case" in {
    val kmeansClusterer = new KMeansClusterer
    val c1 = Seq(MathVector(1, 1))
    val c2 = Seq(MathVector(1, 1), MathVector(2, 2))
    val centroids: Seq[MathVector] = kmeansClusterer.calcCentroids(Set(c1, c2))
    centroids should contain theSameElementsInOrderAs Seq(MathVector(1, 1), MathVector(1.5, 1.5))
  }

  "assign" should "reassign vectors to clusters around closest centroids" in {
    val kmeansClusterer = new KMeansClusterer
    val clusters = kmeansClusterer.assign(input, Seq(MathVector(50), MathVector(0), MathVector(100)))

    clusters should contain theSameElementsAs output
  }

  it should "return empty set for empty clusters" in {
    val kmeansClusterer = new KMeansClusterer
    val clusters: Set[Cluster] = kmeansClusterer.assign(Seq.empty, Seq.empty)
    clusters.size should be(0)
  }

  it should "assign all vectors to a single cluster if only one centroid is given" in {
    val kmeansClusterer = new KMeansClusterer
    val clusters: Set[Cluster] = kmeansClusterer.assign(input, Seq(MathVector(1)))
    clusters.size should be(1)
    clusters.head.size should be(9)
  }

  //   def process(clusters: Set[Cluster], centroids: Seq[MathVector]): Set[Cluster] = {
  "process" should "produce one cluster if one centroid is given" in {
    val kmeansClusterer = new KMeansClusterer

    val clusters: Set[Cluster] = kmeansClusterer.process(Set(input), Seq(MathVector(1)))
    clusters.size should be(1)
    clusters.head.size should be(9)
  }

  it should "correctly split single cluster for given centroids" in {
    val kmeansClusterer = new KMeansClusterer
    val clusters: Set[Cluster] = kmeansClusterer.process(Set(input), Seq(MathVector(1), MathVector(50), MathVector(100)))
    clusters should contain theSameElementsAs output
  }

  "i++" should "return same number of centroids as groups given" in {
    val kmeansClusterer = new KMeansClusterer
    val centroids = kmeansClusterer.initialize_plusplus(input, 3)
    centroids.size should be(3)
  }

  "clusterize" should "return no clusters if there are no points" in {
    val kmeansClusterer = new KMeansClusterer
    val clusters = kmeansClusterer.clusterize(Seq.empty, 1)
    clusters.size should be(0)
  }

  it should "return cluster itself if nClusters is 1" in {
    val kmeansClusterer = new KMeansClusterer
    val clusters: Set[Cluster] = kmeansClusterer.clusterize(input, 1)
    clusters.size should be (1)
    clusters.head should contain theSameElementsAs input
  }

  it should "return proper clusters in generic case" in {
    val rng = new Rng(new Random(100))
    val kmeansClusterer = new KMeansClusterer(rng)
    val clusters: Set[Cluster] = kmeansClusterer.clusterize(input, 3)
    clusters should contain theSameElementsAs output
  }
}
