package models

sealed trait Bracket {
  val size: Int
  lazy val name = s"${size}v${size}"
}

object Twos extends Bracket { val size = 2 }
object Threes extends Bracket { val size = 3 }
object Fives extends Bracket { val size = 5 }
object Rbg extends Bracket {
  val size = 10
  override lazy val name = "rbg"
}
