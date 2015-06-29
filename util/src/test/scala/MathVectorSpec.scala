import info.fotm.util.MathVector
import org.scalatest._

class MathVectorSpec extends FlatSpec with Matchers {

  "MathVector" should "add vectors correctly" in {
    (MathVector(1.0, 1.0) + MathVector(2.0, 2.0)) should be (MathVector(3.0, 3.0))
  }

  it should "throw IllegalArgumentException for vectors of different cardinality" in {
    an [IllegalArgumentException] should be thrownBy {
      MathVector(1.0, 1.0) + MathVector(1.0)
    }
  }

  it should "subtract vectors correctly" in {
    (MathVector(1.0, 1.0) - MathVector(2.0, 2.0)) should be (MathVector(-1.0, -1.0))
  }

  it should "calc dist between vectors" in {
    (MathVector(1.0, 1.0) distTo MathVector(2.0, 2.0)) should be (Math.sqrt(2))
  }

  it should "divide vector by number correctly" in {
    (MathVector(1.0, 1.0) / 2) should be (MathVector(.5, .5))
  }

  it should "multiply vector by a number correctly" in {
    (MathVector(1.0, 1.0) * 2) should be (MathVector(2, 2))
  }

  it should "calc vector length correctly" in {
    MathVector(3.0, 4.0).length should be (5.0)
  }

  it should "normalize vector correctly" in {
    MathVector(2.0, 0, 0).normalize should be (MathVector(1.0, 0, 0))
  }
}
