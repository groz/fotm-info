import scodec._
import scodec.bits._
import codecs._

case class Point(x: Int, y: Int, z: Int)

val pts = List(
  Point(1, 2, 3),
  Point(4, 5, 6)
)

val binaryPoint: BitVector = Codec.encode(pts).require
