/*
import org.scalatest.matchers.{MatchResult, Matcher}

case class TolerantEqualMatcher(seq1: Seq[Seq[Double]], eps: Double) extends Matcher[Double]
{
  def apply(seq2: Seq[Seq[Double]]): MatchResult =
  {
    def wrongSizeResult = MatchResult(false, s"sizes of $seq1 and $seq2 should be equal" , s"sizes of $seq1 and $seq2 should not be equal")
    if (seq1.length != seq2.length)
      wrongSizeResult
    else
    {
      for (i <- 0 until seq1.length)
      {
        if (seq1(i).length != seq2(i).length)
        {
          return wrongSizeResult
        }

        seq1(i).zip(seq2(i))
        ???
      }
      ???
    }

  }
  def tolerantEqual(seq1: Seq[Seq[Double]], eps: Double) = TolerantEqualMatcher(seq1, eps)
}
*/
