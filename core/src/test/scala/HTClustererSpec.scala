import info.fotm.clustering.implementations.HTClusterer
import info.fotm.util.MathVector
import org.scalatest._

/**
 * Created by Hasan on 29.07.2015.
 */
class HTClustererSpec extends FlatSpec with Matchers with ClustererSpecBase {
  var htc3 = new HTClusterer()

  "distTo" should "equal to sqrt(2)" in {
    val a = new MathVector(Seq(0, 0))
    val v1 = new MathVector(Seq(1, 1))
    val v2 = new MathVector(Seq(-1, 1))
    val v3 = new MathVector(Seq(-1, -1))
    val v4 = new MathVector(Seq(1, -1))

    val d : Double = htc3.distTo(a, Seq(v1, v2, v3, v4))

    d should be(math.sqrt(2))
  }

  it should "throw exception on empty set" in {
    a [UnsupportedOperationException] should be thrownBy {
      htc3.distTo(new MathVector(Seq(0,1)), Seq())
    }
  }

  "clusterize" should "works correctly" in {
    val x = new HTClusterer()
    val x11 = new MathVector(List(1.1, 1.0))
    val x12 = new MathVector(List(1.0, 1.1))
    val x13 = new MathVector(List(0.9, 1.1))
    val x21 = new MathVector(List(-1.1, 1.0))
    val x22 = new MathVector(List(-1.2, 0.9))
    val x23 = new MathVector(List(-1.0, 1.1))
    val x31 = new MathVector(List(1.1, -1.0))
    val x32 = new MathVector(List(1.0, -1.1))
    val x33 = new MathVector(List(1.0, -0.9))
    val x41 = new MathVector(List(-1.1, -0.9))
    val x42 = new MathVector(List(-1.0, -1.1))
    val x43 = new MathVector(List(-0.9, -1.1))
    val set = Seq(x11, x12, x13, x21, x22, x23, x31, x32, x33, x41, x42, x43)
    val r1 = x.clusterize(set, 3).map(x => x.sortBy(y => y(0) * 1000 + y(1)))
    val r2 = Set(Seq(x11, x12, x13), Seq(x21, x22, x23), Seq(x31, x32, x33), Seq(x41, x42, x43)).map(x => x.sortBy(y => y(0) * 1000 + y(1)))
    (r1 == r2) should be (true)
  }


}
