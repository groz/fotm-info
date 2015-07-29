import info.fotm.clustering.implementations.HTClusterer3
import info.fotm.util.MathVector
import org.scalatest._

/**
 * Created by Hasan on 29.07.2015.
 */
class HTClusterer3Spec extends FlatSpec with Matchers with ClustererSpecBase {
  var htc3 = new HTClusterer3()

  "Distance from (0,0) to [(1,1), (-1,1), (-1,-1), (1,-1)]" should "equals to sqrt(2)" in {
    val a = new MathVector(Seq(0, 0))
    val v1 = new MathVector(Seq(1, 1))
    val v2 = new MathVector(Seq(-1, 1))
    val v3 = new MathVector(Seq(-1, -1))
    val v4 = new MathVector(Seq(1, -1))

    val d : Double = htc3.distTo(a, Seq(v1, v2, v3, v4))

    d should be(math.sqrt(2))
  }
}
