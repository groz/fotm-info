package info.fotm.util

import scala.collection.generic.CanBuildFrom

trait RandomNumberGenerator {
  def nextInt(): Int
  def nextInt(max: Int): Int
  def nextDouble(): Double
  def shuffle[T, CC[X] <: TraversableOnce[X]](xs: CC[T])(implicit bf: CanBuildFrom[CC[T], T, CC[T]]): CC[T]
}

class Rng(rng: scala.util.Random) extends RandomNumberGenerator {
  def this() = this(new scala.util.Random)
  override def nextInt(): Int = rng.nextInt()
  override def nextInt(max: Int): Int = rng.nextInt(max)
  override def nextDouble(): Double = rng.nextDouble()
  override def shuffle[T, CC[X] <: TraversableOnce[X]](xs: CC[T])(implicit bf: CanBuildFrom[CC[T], T, CC[T]]): CC[T] =
    rng.shuffle(xs)(bf)
}

class NullRng extends RandomNumberGenerator {
  override def nextInt(): Int = 0
  override def nextInt(max: Int): Int = 0
  override def nextDouble(): Double = 0
  override def shuffle[T, CC[X] <: TraversableOnce[X]](xs: CC[T])(implicit bf: CanBuildFrom[CC[T], T, CC[T]]): CC[T] = xs
}
