import info.fotm.domain._
import play.api.libs.json._
type StorageState = Seq[(Axis, AxisState)]
import JsonFormatters._

val x: StorageState = Seq(
  Axis.all.head -> AxisState(Set(), Set(), Seq(), Seq())
)
Json.toJson(x)
