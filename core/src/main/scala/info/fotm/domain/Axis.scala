package info.fotm.domain

import info.fotm.api.models._

case object Axis {
  val allRegions = List(US, Europe, China, Korea, Taiwan)
  val allBrackets = List(Twos, Threes, Fives, Rbg)

  val all = for {
    region <- allRegions
    bracket <- allBrackets
  } yield Axis(region, bracket)
}

final case class Axis(region: Region, bracket: Bracket)

