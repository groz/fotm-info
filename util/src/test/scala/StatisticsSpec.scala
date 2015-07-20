import info.fotm.util.MathVector
import info.fotm.util.Statistics._
import org.scalatest._

class StatisticsSpec extends FlatSpec with Matchers {

  "F-score" should "output 1 for all TP hits" in {
    f1Score(Metrics(100, 0, 0)) should be(1.0)
  }

  it should "output 0 for all FP misses" in {
    f1Score(Metrics(0, 100, 0)) should be(0.0)
  }

  it should "output 0 for all FN misses" in {
    f1Score(Metrics(0, 0, 100)) should be(0.0)
  }

  it should "output 2/3 for 50/50 FP misses" in {
    f1Score(Metrics(50, 50, 0)) should be(2 / 3.0)
  }

  it should "output 2/3 for 50/50 FN misses" in {
    f1Score(Metrics(50, 0, 50)) should be(2 / 3.0)
  }

  "calcMetrics" should "output correct numbers" in {
    val metrics = calcMetrics(Set(0, 1, 2, 3), Set(1, 2, 3, 4, 5))
    // counts
    metrics.truePositive should be(3)
    metrics.falsePositive should be(1)
    metrics.falseNegative should be(2)
  }

  it should "combine with other metrics correctly" in {
    (Metrics(1, 2, 3) + Metrics(3, 4, 5)) should be(Metrics(4, 6, 8))
  }

  "normalize" should "correctly scale matrix" in {
    implicit val comparer = new org.scalactic.Equality[MathVector] {
      override def areEqual(a: MathVector, b: Any): Boolean =
        b.isInstanceOf[MathVector] && a.coords == b.asInstanceOf[MathVector].coords
    }

    val input = Seq(
      MathVector(  0,  0, 0),
      MathVector( 50,  5, 0.5),
      MathVector(100, 10, 1)
    )

    val expected = Seq(
      MathVector(-0.5, -0.5, -0.5),
      MathVector(0, 0, 0),
      MathVector(0.5, 0.5, 0.5)
    )

    val normalized = normalize(input)
    normalized should contain theSameElementsInOrderAs (expected)
  }

}
