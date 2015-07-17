package info.fotm.clustering

import info.fotm.util.MathVector

class Feature[T](name: String, extractor: T => Double, weight: Double = 1.0) {
  def apply(value: T) = extractor(value)
}

object Feature {
  def apply[T](name: String, extractor: T => Double, weight: Double = 1.0): Feature[T] =
    new Feature[T](name, extractor, weight)

  def calcVector[T](obj: T, features: List[Feature[T]]): MathVector = MathVector(features.map(f => f(obj)): _*)
}
