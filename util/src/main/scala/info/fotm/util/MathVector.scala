package info.fotm.util

class MathVector(val coords: Seq[Double]) {
  private def operation(that: MathVector, op: (Double, Double) => Double): MathVector = {
    require(this.dimension == that.dimension)
    val seq = coords.view.zip(that.coords).map(c => op(c._1, c._2))
    MathVector(seq: _*)
  }

  lazy val dimension = coords.length

  lazy val length: Double = Math.sqrt(sqrlength)

  lazy val sqrlength: Double = coords.view.map(x => x * x).sum

  lazy val unary_- = MathVector(coords.map(-1 * _): _*)

  override lazy val toString = "MathVector"+coords.mkString("(", ",", ")")

  def unary_+ = this

  def +(that: MathVector) = operation(that, _ + _)

  def -(that: MathVector) = operation(that, _ - _)

  def scale(that: MathVector) = operation(that, _ * _)

  def scalar_*(that: MathVector) = scale(that).coords.sum

  def *(x: Double) = MathVector(coords.map(_ * x): _*)

  def /(x: Double) = this * (1.0 / x)

  def distImpl(that: MathVector, fn: Double => Double): Double = {
    val iter1 = coords.iterator
    val iter2 = that.coords.iterator
    var sum = 0.0
    while (iter1.hasNext) {
      val d: Double = iter1.next() - iter2.next()
      sum += fn(d)
    }
    sum
  }

  def distTo(that: MathVector): Double = Math.sqrt(distImpl(that, x => x * x))

  def manhattanDist(that: MathVector): Double = distImpl(that, Math.abs)

  def distToInfty(that: MathVector): Double = (this - that).coords.map(math.abs).max

  def normalize: MathVector = MathVector(coords.map(x => x / length): _*)

  def update(n: Int, x: Double): MathVector = MathVector(coords.patch(n, Seq(x), 1): _*)

  def apply(n: Int): Double = coords(n)

  def distTo(vectors: Iterable[MathVector]): Double = vectors.map(_ distTo this).min
}

object MathVector {
  def apply(coords: Double*) = new MathVector(coords)
  def avg(vectors: Iterable[MathVector]) = vectors.reduce(_ + _) / vectors.size
  implicit def doubleToMathVectorAsScalar(i: Double): MathVectorAsScalar = new MathVectorAsScalar(i)
  implicit def doubleToMathVector(i: Double): MathVector = MathVector(i)
}

class MathVectorAsScalar(i: Double) {
  def *(v: MathVector) = v * i
}
