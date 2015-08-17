package info.fotm

import info.fotm.domain._
import scodec._
import scodec.bits._
import codecs._
import scodec.codecs.implicits._
import com.github.nscala_time.time.Imports._
import scala.collection.immutable.TreeMap

object MyCodecImplicits {

  implicit def seqCodec[A](implicit listCodec: Codec[List[A]]): Codec[Seq[A]] =
    listCodec.xmap(_.toSeq, _.toList)

  implicit def setCodec[A](implicit listCodec: Codec[List[A]]): Codec[Set[A]] =
    listCodec.xmap(_.toSet[A], _.toList)

  implicit def mapCodec[K, V](implicit listCodec: Codec[List[(K, V)]]): Codec[Map[K, V]] =
    listCodec.xmap(_.toMap[K, V], _.toList)

  implicit def treemapCodec[K, V](implicit listCodec: Codec[List[(K, V)]], ordering: Ordering[K]): Codec[TreeMap[K, V]] =
    listCodec.xmap(lst => TreeMap(lst: _*), _.toList)

  implicit def datetimeCodec(implicit longCodec: Codec[Long]): Codec[DateTime] =
    longCodec.xmap(new DateTime(_), _.toInstant.getMillis)

  implicit def axisCodec(implicit strCodec: Codec[String]): Codec[Axis] =
    strCodec.xmap(
      str => {
        val Array(region, bracket) = str.split(',')
        Axis.parse(region, bracket).get
      },
      axis => s"${axis.region.slug},${axis.bracket.slug}")
}
