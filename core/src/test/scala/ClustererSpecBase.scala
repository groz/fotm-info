import info.fotm.util.MathVector
import org.scalactic.Equality

trait ClustererSpecBase {
  implicit val comparer: Equality[Seq[MathVector]] = new Equality[Seq[MathVector]] {
    override def areEqual(a: Seq[MathVector], b: Any): Boolean =
      b.isInstanceOf[Seq[MathVector]] && b.asInstanceOf[Seq[MathVector]].toSet == a.toSet
  }
}
