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

  def mean(seq: Seq[Double]): Double = seq.sorted.apply(seq.size / 2)

  def normalize(matrix: Seq[MathVector]): Seq[MathVector] = {
    val mt = matrix.map(_.coords).transpose
    val min_mean_max = mt.map(column => (column.min, mean(column), column.max))

    val scaled = for {
      (column, (min, mean, max)) <- mt.zip(min_mean_max)
    } yield column.map { v =>
        if (max != min) (v - mean) / (max - min)
        else 0.0
      }

    scaled.transpose.map(row => MathVector(row: _*))
  }

  /**
   * Gets random value from the sequence according to weights
   * @param input   Sequence of elements
   * @param weights Weights
   * @param rnd     Random number in [0.0, 1.0)
   * @tparam T      Type of sequence elements
   * @return        Random value according to probabilites
   */
  def randomWeightedValue[T](input: Seq[T], weights: Seq[Double], rnd: Double): T = {
    val distribution = weights.scanLeft(0.0) {_ + _}.tail
    val rand: Double = rnd * distribution.last
    val idx: Int = distribution.takeWhile(_ < rand).size
    input(idx)
  }
}
