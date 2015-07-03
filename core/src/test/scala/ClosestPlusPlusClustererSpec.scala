import info.fotm.clustering._
import info.fotm.util.MathVector
import org.scalactic.Equality
import org.scalatest._

import scala.collection.immutable.{TreeMap, IndexedSeq}

class ClosestPlusPlusClustererSpec extends FlatSpec with Matchers with ClustererSpecBase {
  val clusterer = new ClosestPlusPlusClusterer

  val input = Seq(
    MathVector(0, 0),
    MathVector(50, 50),
    MathVector(100, 100),
    MathVector(1, 1),
    MathVector(51, 51),
    MathVector(101, 101)
  )

  "init" should "correctly init clusters" in {
    val expected = Seq(
      Seq(input(0)),
      Seq(input(5)),
      Seq(input(1))
    )

    val (clusters, _) = clusterer.init(input, 2)

    clusters should contain theSameElementsAs expected
  }

  "init" should "correctly remove cluster points" in {
    val expected = input diff List(0, 1, 5).map(input)

    val (_, points) = clusterer.init(input, 2)

    points should contain theSameElementsAs expected
  }

  "clusterize" should "correctly group simple vectors" in {
    val expected = Set(
      Seq(input(0), input(3)),
      Seq(input(1), input(4)),
      Seq(input(2), input(5))
    )

    val clusters: Set[Seq[MathVector]] = clusterer.clusterize(input, 2)

    expected.foreach(clusters should contain (_))
  }

}