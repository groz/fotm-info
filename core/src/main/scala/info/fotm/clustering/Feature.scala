package info.fotm.clustering

import info.fotm.util.{Statistics, MathVector}

case class Feature[T](name: String, extract: T => Double, weight: Double = 1.0)

object Feature {

  def const[T] = new Feature[T]("const", _ => 1)

  def calcVector[T](obj: T, features: List[Feature[T]]): MathVector =
    MathVector(features.map(f => f.extract(obj)): _*)

  def normalize[T](features: List[Feature[T]], ts: Seq[T]): Seq[MathVector] = {
    val matrix = ts.map(t => calcVector(t, features))
    val normalized: Seq[MathVector] = Statistics.normalize(matrix)
    normalized.map { nv =>
      val coords = nv.coords.zip(features).map(kv => kv._1 * kv._2.weight)
      MathVector(coords: _*)
    }
  }

  def reweigh[T](featuresWithWeights: Seq[(Feature[T], Double)]) =
    for { (f: Feature[T], w) <- featuresWithWeights }
    yield Feature[T](f.name, f.extract, w)

}
