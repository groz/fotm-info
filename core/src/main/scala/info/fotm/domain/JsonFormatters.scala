package info.fotm.domain

import play.api.libs.functional.syntax._
import play.api.libs.json._

object JsonFormatters {
  lazy implicit val statsFmt = Json.format[Stats]
  lazy implicit val charIdFmt = Json.format[CharacterId]
  lazy implicit val charStatsFmt = Json.format[CharacterStats]
  lazy implicit val charViewFmt = Json.format[CharacterView]
  lazy implicit val charSsFmt = Json.format[CharacterSnapshot]

  lazy implicit val teamFmt: Format[Team] =
    (__ \ "members").format[Seq[CharacterId]].inmap(
      chars => Team(chars.toSet),
      team => team.members.toSeq.sortBy(id => (id.name, id.realmSlug))
    )

  lazy implicit val teamViewFmt = Json.format[TeamView]
  lazy implicit val teamSsFmt = Json.format[TeamSnapshot]

  lazy implicit val axisFmt: Format[Axis] = (
    (JsPath \ "region").format[String] and
    (JsPath \ "bracket").format[String]
  )(
    (r, b) => Axis.parse(r, b).get,
    a => (a.region.slug, a.bracket.slug)
  )

  implicit def pairsFmt[A, B]
  (implicit fmtA: Format[A], fmtB: Format[B])
  : Format[(A, B)] = (
    (JsPath \ "_1").format[A] and
    (JsPath \ "_2").format[B]
  )(Tuple2.apply[A, B], unlift(Tuple2.unapply))

  lazy implicit val axisStateFmt = Json.format[AxisState]

  def oformat[A](implicit fmt: Format[A]): OFormat[A] =
    OFormat(fmt, OWrites[A](a => fmt.writes(a).asInstanceOf[JsObject]))
}
