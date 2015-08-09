import info.fotm.util.MathVector

object ComparerTestHelpers {

  implicit val comparer = new org.scalactic.Equality[MathVector] {
    override def areEqual(a: MathVector, b: Any): Boolean =
      b.isInstanceOf[MathVector] && a.coords == b.asInstanceOf[MathVector].coords
  }

  implicit val seqComparer = new org.scalactic.Equality[Seq[MathVector]] {
    override def areEqual(as: Seq[MathVector], b: Any): Boolean = {
      val bs = b.asInstanceOf[Seq[MathVector]]
      as.size == bs.size &&
        as.forall(a => bs.exists(b => comparer.areEqual(a, b)))
    }
  }
}
