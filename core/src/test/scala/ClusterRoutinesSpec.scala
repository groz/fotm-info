import info.fotm.clustering.Clusterer.Cluster
import info.fotm.clustering.implementations.RMClustering.ClusterRoutines._
import info.fotm.util.MathVector
import org.scalatest.{Matchers, FlatSpec}

class ClusterRoutinesSpec extends FlatSpec with Matchers
{
  val eps = 1E-10

  trait Clusters
  {
    val groupSize = 2
    val clusters: List[Cluster] = List(
      Seq(MathVector(0, 0), MathVector(1, 1)),
      Seq(MathVector(5, 6), MathVector(6, 7), MathVector(8, 8)),
      Seq(MathVector(10, 2), MathVector(11, 3)),
      Seq(MathVector(1, 11))
    )
  }

  trait Graph extends Clusters
  {
    val graph = makeGraphFromClusters(clusters, groupSize)
  }


  "makeGraphFromClusters" should "correctly create graph" in new Graph
  {
    withClue("Labels count: ")
    {
      graph.labels should have size graph.vertexCount
    }
    withClue("Labels: ")
    {
      graph.labels should equal(Map(0 -> 0, 1 -> 1, 2 -> 0, 3 -> -1).toMap)
    }
    withClue("Graph matrix: ")
    {
      graph.matrix should equal(Vector(
        Vector(0.0, 9.0, 10.0, 10.0),
        Vector(9.0, 0.0, 8.0, 9.0),
        Vector(10.0, 8.0, 0.0, 18.0),
        Vector(10.0, 9.0, 18.0, 0.0)
      ))
    }

  }

  it should "correctly create empty graphs" in
  {
    val clusters = List()
    val graph = makeGraphFromClusters(clusters, 3)

    graph.vertexCount should equal(0)
    graph.labels should have size (0)
    graph.matrix should have size (0)
  }

  it should "correctly process cluster list with empty cluster" in new Clusters
  {
    val clusters2 = clusters ++ Seq(Seq())
    the [IllegalArgumentException] thrownBy
    {
      makeGraphFromClusters(clusters2, 2)
    } should have message "Clusters can't contain empty one"

  }


}
