package info.fotm.util

object Statistics {

  // https://en.wikipedia.org/wiki/F1_score
  def fScore(beta: Double)(m: Metrics) = {
    import m._
    val betaSquared = beta * beta
    val nom = (1 + betaSquared) * truePositive
    nom / (nom + betaSquared * falseNegative + falsePositive)
  }

  def f1Score = fScore(1.0) _

  case class Metrics(truePositive: Int, falsePositive: Int, falseNegative: Int) {
    def +(that: Metrics) = Metrics(
      truePositive + that.truePositive,
      falsePositive + that.falsePositive,
      falseNegative + that.falseNegative)
  }

  def calcMetrics[T](result: Set[T], source: Set[T]) = {
    val truePositive  = result.intersect(source)
    val falsePositive = result -- truePositive
    val falseNegative = source -- result
    Metrics(truePositive.size, falsePositive.size, falseNegative.size)
  }

  def normalize(matrix: Seq[MathVector]): Seq[MathVector] = {
    val mt = matrix.map(_.coords).transpose
    val minmax = mt.map(column => (column.min, column.max))

    val scaled = for {
      (column, (min, max)) <- mt.zip(minmax)
    } yield column.map { v =>
        if (max - min != 0)
          (v - min) / (max - min)
        else
          0.0
      }

    scaled.transpose.map(row => MathVector(row: _*))
  }
}
