import info.fotm.clustering._
import info.fotm.util.MathVector
import org.scalatest._

import scala.collection.immutable.{TreeMap, IndexedSeq}

class ClosestClustererSpec extends FlatSpec with Matchers {

  "clusterize" should "correctly group simple vectors" in {
    val clusterer = new ClosestClusterer

    val input = Seq(
      MathVector(0, 0),
      MathVector(50, 50),
      MathVector(100, 100),
      MathVector(1, 1),
      MathVector(51, 51),
      MathVector(101, 101)
    )

    val expected = Set(
      Seq(input(0), input(3)),
      Seq(input(1), input(4)),
      Seq(input(2), input(5))
    )

    val clusters: Set[Seq[MathVector]] = clusterer.clusterize(input, 2)

    clusters should contain theSameElementsAs expected
  }

}