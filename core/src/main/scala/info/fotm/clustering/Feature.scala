package info.fotm.clustering

final case class Feature[T](name: String, extractor: T => Double, weight: Double) {
  def apply(value: T) = extractor(value)
}
