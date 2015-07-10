package info.fotm.util

import com.twitter.bijection.Bijection
import java.nio.charset.StandardCharsets.UTF_8

object Compression {

  implicit val str2bytes = Bijection.build[String, Array[Byte]](_.getBytes(UTF_8))(new String(_, UTF_8))

  implicit val str2base64 = str2bytes andThen Bijection.bytes2Base64

  implicit val str2GZippedBase64 = str2bytes andThen Bijection.bytes2GZippedBase64

}
