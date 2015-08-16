package info.fotm

import com.github.nscala_time.time.Imports._
import com.twitter.bijection.Bijection
import info.fotm.aether.Storage.PersistedStorageState
import info.fotm.aether.Storage
import info.fotm.domain._
import info.fotm.util.S3Persisted
import scodec._
import scodec.bits._
import codecs._
import scodec.codecs.implicits._
import com.github.nscala_time.time.Imports._
import scala.collection.breakOut

object MyCodecImplicits {

  implicit def seqCodec[A](implicit listCodec: Codec[List[A]]): Codec[Seq[A]] =
    listCodec.xmap(_.toSeq, _.toList)

  implicit def setCodec[A](implicit listCodec: Codec[List[A]]): Codec[Set[A]] =
    listCodec.xmap(_.toSet[A], _.toList)

  implicit def mapCodec[K, V](implicit listCodec: Codec[List[(K, V)]]): Codec[Map[K, V]] =
    listCodec.xmap(_.toMap[K, V], _.toList)

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


object SerApp extends App {
  import MyCodecImplicits._

  println("Fetching from S3...")

  val bucket = "fotm-info-staging-bucket"
  val path = "storage.txt"
  val p = new S3Persisted[PersistedStorageState](bucket, path)(Storage.serializer)
  val state: PersistedStorageState = p.fetch().get

  println("S3 download complete.")

  val jsonStart = DateTime.now

  val gzippedJson = Storage.serializer(state)

  val elapsedJson = jsonStart.to(DateTime.now)
  println(s"Gzipped64 Json size: ${gzippedJson.length}, time: $elapsedJson")

  val start = DateTime.now

  val serialized = Codec.encode(state).require
  Codec.decode[PersistedStorageState](serialized).require.value
  val gzipped = Bijection.bytes2GzippedBytes(serialized.toByteArray)

  val elapsed = start.to(DateTime.now)

  println(s"Serialized size: ${serialized.size}, gzipped size: ${gzipped.bytes.length}, time: $elapsed")

}
