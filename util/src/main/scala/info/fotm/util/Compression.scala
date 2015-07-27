package info.fotm.util

import com.twitter.bijection.{GZippedBase64String, Bijection}
import java.nio.charset.StandardCharsets.UTF_8

object Compression {

  implicit val str2bytes = Bijection.build[String, Array[Byte]](_.getBytes)(new String(_))

  implicit val str2base64 = str2bytes andThen Bijection.bytes2Base64

  implicit val str2GZippedBase64 = str2bytes andThen Bijection.bytes2GZippedBase64

  val str2rawGZipBase64 = Bijection.build[String, GZippedBase64String](s => GZippedBase64String(s))(_.str)

}
