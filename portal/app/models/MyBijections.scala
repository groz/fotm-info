package models

import com.twitter.bijection.{Base64String, Bijection, GZippedBytes}
import info.fotm.domain.{CharacterId, Team}
import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs.implicits._
import info.fotm.MyCodecImplicits._

object MyBijections {
  def scodecBase64Bijection[T](implicit codec: Codec[T]): Bijection[T, String] = Bijection.build[T, String] { t =>
    val bytes = Codec.encode(t).require.toByteArray
    Bijection.bytes2Base64(bytes).str
  } { base64 =>
    val bytes = Bijection.bytes2Base64.inverse(Base64String(base64))
    Codec.decode[T](BitVector(bytes)).require.value
  }

  def scodecGzipBijection[T](implicit codec: Codec[T]): Bijection[T, Array[Byte]] = Bijection.build[T, Array[Byte]] { t =>
    val bytes = Codec.encode(t).require.toByteArray
    Bijection.bytes2GzippedBytes(bytes).bytes
  } { gzippedBytes =>
    val bytes = Bijection.bytes2GzippedBytes.inverse(GZippedBytes(gzippedBytes))
    Codec.decode[T](BitVector(bytes)).require.value
  }

  lazy val teamIdBijection: Bijection[Team, String] = scodecBase64Bijection[Team]
  lazy val charIdBijection: Bijection[CharacterId, String] = scodecBase64Bijection[CharacterId]
}
