package info.fotm.util

class MathVector(val coords: Seq[Double]) {
  private def operation(that: MathVector, coordsOp: (Double, Double) => Double) = {
    require(this.dimension == that.dimension)
    MathVector(coords.zip(that.coords).map(c => coordsOp(c._1, c._2)): _*)
  }

  lazy val dimension = coords.length

  lazy val length: Double = Math.sqrt(sqrlength)

  lazy val sqrlength: Double = coords.map(Math.pow(_, 2)).sum

  lazy val unary_- = MathVector(coords.map(-1 * _): _*)

  override lazy val toString = "MathVector"+coords.mkString("(", ",", ")")

  def unary_+ = this

  def +(that: MathVector) = operation(that, _ + _)

  def -(that: MathVector) = operation(that, _ - _)

  def scalar_*(that: MathVector) = operation(that, _ * _).coords.sum

  def *(x: Double) = MathVector(coords.map(_ * x): _*)

  def /(x: Double) = this * (1.0 / x)

  def distTo(that: MathVector) = (this - that).length

  def distTo1(that: MathVector) = (this - that).coords.map(math.abs).sum

  def distToInfty(that: MathVector) = (this - that).coords.maxBy(math.abs).abs
  def sqrDistTo(that: MathVector) = (this - that).sqrlength

  def normalize: MathVector = MathVector(coords.map(x => x / length): _*)
}

object MathVector {
  def apply(coords: Double*) = new MathVector(coords)
  def avg(vectors: Iterable[MathVector]) = vectors.reduce(_ + _) / vectors.size
  implicit def doubleToMathVectorAsScalar(i: Double): MathVectorAsScalar = new MathVectorAsScalar(i)
}

class MathVectorAsScalar(i: Double) {
  def *(v: MathVector) = v * i
}
