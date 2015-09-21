package info.fotm.domain

import info.fotm.api.models._

case object Axis {
  val parsedRegions = List(US, Europe, China, Korea, Taiwan)
  val parsedBrackets = List(Twos, Threes, Fives, Rbg)

  val allRegions = List(US, Europe, Korea, China, Taiwan)
  val allBrackets = List(Twos, Threes, Fives, Rbg)

  val regionMap: Map[String, Region] = parsedRegions.map(r => (r.slug, r)).toMap

  val bracketsMap: Map[String, Bracket] = parsedBrackets.map(b => (b.slug, b)).toMap

  val all = for {
    region <- allRegions
    bracket <- allBrackets
  } yield Axis(region, bracket)

  def parse(regionSlug: String, bracketSlug: String): Option[Axis] =
    for {
      r <- regionMap.get(regionSlug)
      b <- bracketsMap.get(bracketSlug)
    } yield Axis(r, b)
}

final case class Axis(region: Region, bracket: Bracket)

