import info.fotm.clustering.Clusterer.Cluster
import info.fotm.clustering.implementations.RMClustering.KMeansDiv2Clusterer
import info.fotm.util.MathVector
import org.scalatest.{Matchers, FlatSpec}

import ComparerTestHelpers._

class KMeansDiv2ClustererSpec extends FlatSpec with Matchers {

  val input: Seq[MathVector] = Seq(0.0, 1.0, 2.0, 3.0, 100.0, 101.0)

  "split" should "correctly divide points into two clusters" in {
    val clusterer = new KMeansDiv2Clusterer()
    val clusters: Set[Cluster] = clusterer.split(input)
    clusters.size should be(2)
    clusters should contain theSameElementsAs Set[Cluster](
      Seq(0.0, 1.0, 2.0, 3.0),
      Seq(100.0, 101.0)
    )
  }

  it should "return empty set if no points are provided" in {
    val clusterer = new KMeansDiv2Clusterer()
    val clusters: Set[Cluster] = clusterer.split(Seq.empty)
    clusters.size should be(0)
  }

  it should "return single-element set if one point is provided" in {
    val clusterer = new KMeansDiv2Clusterer()
    val clusters: Set[Cluster] = clusterer.split(Seq(1.0))
    clusters.size should be(1)
    clusters.head should equal(Seq(MathVector(1.0)))
  }

  it should "split set even if points collide" in {
    val clusterer = new KMeansDiv2Clusterer()
    val clusters: Set[Cluster] = clusterer.split(Seq(1.0, 1.0, 1.0))
    clusters.size should be(2)
  }

  "clusterizationStep" should "leave clusters in tact if there's already a quorum" in {
    val clusterer = new KMeansDiv2Clusterer()
    val cluster1: Seq[MathVector] = Seq(0.0, 1.0, 2.0, 3.0)
    val cluster2: Seq[MathVector] = Seq(100.0, 101.0)
    val output = clusterer.clusterizationStep(Set(cluster1, cluster2), 2)
    output.size should be(2)
  }

  it should "divide bigger cluster" in {
    val clusterer = new KMeansDiv2Clusterer(_.maxBy(_.size))
    val cluster1: Seq[MathVector] = Seq(0.0, 1.0, 50.0, 51.0)
    val cluster2: Seq[MathVector] = Seq(100.0, 101.0)
    val output = clusterer.clusterizationStep(Set(cluster1, cluster2), 3)

    output.size should be(3)

    output should contain theSameElementsAs Seq(
      Seq[MathVector](0.0, 1.0),
      Seq[MathVector](50.0, 51.0),
      Seq[MathVector](100.0, 101.0)
    )
  }

}
