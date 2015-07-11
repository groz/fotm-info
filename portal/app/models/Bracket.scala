package models

sealed case class Bracket(size: Int) {
  lazy val name = s"${size}v${size}"
}

object Twos extends Bracket(2)
object Threes extends Bracket(3)
object Fives extends Bracket(5)
object Rbg extends Bracket(10) {
  override lazy val name = "rbg"
}
