package info.fotm.clustering

import info.fotm.util.MathVector

object ML {

  def partialDerivative(f: MathVector => Double, point: MathVector, n: Int): Double = {
    val dx = 5e-2
    val nextPoint = MathVector(point.coords.patch(n, Seq(point.coords(n)+dx), 1): _*)
    (f(nextPoint) - f(point)) / dx
  }

  def gradientDescent(
    start: MathVector,
    costFunction: (MathVector => Double),
    learningRate: Double,
    nIterations: Int = 1000): MathVector = {

    (1 to nIterations).foldLeft(start) { (theta, i) =>
      println(theta)

      val nextCoords =
        for { (t, pos) <- theta.coords.zipWithIndex.par }
        yield t - learningRate * partialDerivative(costFunction, theta, pos)

      MathVector(nextCoords.toList: _*)
    }
  }

  def findWeights[T](features: Seq[Feature[T]], estimate: (Seq[Feature[T]] => Double)): MathVector = {
    val weights: MathVector = MathVector(features.map(_.weight): _*)
    gradientDescent(weights, w => {
      val fs = Feature.reweigh(features.zip(w.coords))
      estimate(fs)
    }, learningRate = 0.1)
  }
}
