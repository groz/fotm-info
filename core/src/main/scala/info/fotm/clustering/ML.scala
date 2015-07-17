package info.fotm.clustering

import info.fotm.util.MathVector

object ML {

  def partialDerivative(f: MathVector => Double, point: MathVector, n: Int): Double = {
    val dx = 1e-2
    val nextPoint = MathVector(point.coords.patch(n, Seq(point.coords(n)+dx), 1): _*)
    (f(nextPoint) - f(point)) / dx
  }

  def gradientDescent(
    start: MathVector,
    costFunction: (MathVector => Double),
    learningRate: Double,
    nIterations: Int = 1000): MathVector = {

    var theta = start

    for (i <- 1 to nIterations) {
      val nextCoords = for {
        (t, pos) <- theta.coords.zipWithIndex
      } yield {
        t - learningRate * partialDerivative(costFunction, theta, pos)
      }
      theta = MathVector(nextCoords: _*)
      println(theta)
    }
    theta
  }

  def findWeights[T](features: Seq[Feature[T]], estimate: (Seq[Feature[T]] => Double)): MathVector = {
    val weights: MathVector = MathVector(features.map(_.weight): _*)
    gradientDescent(weights, w => {
      val fs = Feature.reweigh(features.zip(w.coords))
      estimate(fs)
    }, learningRate = 0.2)
  }
}
