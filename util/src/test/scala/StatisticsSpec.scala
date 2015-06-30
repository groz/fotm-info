import info.fotm.util.Statistics._
import org.scalatest._

class StatisticsSpec extends FlatSpec with Matchers {

  "F-score" should "output 1 for all TP hits" in {
    f1Score(100, 0, 0) should be(1.0)
  }

  it should "output 0 for all FP misses" in {
    f1Score(0, 100, 0) should be(0.0)
  }

  it should "output 0 for all FN misses" in {
    f1Score(0, 0, 100) should be(0.0)
  }

  it should "output 2/3 for 50/50 FP misses" in {
    f1Score(50, 50, 0) should be(2/3.0)
  }

  it should "output 2/3 for 50/50 FN misses" in {
    f1Score(50, 0, 50) should be(2/3.0)
  }

}
