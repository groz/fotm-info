import info.fotm.clustering._
import info.fotm.clustering.implementations.ClosestPlusPlusClusterer
import info.fotm.util.MathVector
import org.scalactic.Equality
import org.scalatest._

import scala.collection.immutable.{TreeMap, IndexedSeq}

class ClosestPlusPlusClustererSpec extends FlatSpec with Matchers with ClustererSpecBase {
  val clusterer = new ClosestPlusPlusClusterer

  val input = Seq(
    MathVector(10, 0, 0),   // 0
    MathVector(11, 0, 0),   // 1
    MathVector(12, 0, 0),   // 2

    MathVector(0, 10, 0),   // 3
    MathVector(0, 11, 0),   // 4
    MathVector(0, 12, 0),   // 5

    MathVector(0, 0, 10),   // 6
    MathVector(0, 0, 11),   // 7
    MathVector(0, 0, 12)    // 8
  )

  "init" should "correctly init clusters" in {
    val expected = Seq(
      Seq(input(0)),
      Seq(input(5)),
      Seq(input(8))
    )

    val (clusters, _) = clusterer.init(input, 3)

    clusters should contain theSameElementsAs expected
  }

  "init" should "correctly remove cluster points" in {
    val expected = input diff List(0, 5, 8).map(input)

    val (_, points) = clusterer.init(input, 3)

    points should contain theSameElementsAs expected
  }

  "clusterize" should "correctly group simple vectors" in {
    val expected = Set(
      Seq(0, 1, 2).map(input),
      Seq(3, 4, 5).map(input),
      Seq(6, 7, 8).map(input)
    )

    val clusters: Set[Seq[MathVector]] = clusterer.clusterize(input, 3)

    expected.foreach(clusters should contain (_))
  }

}
