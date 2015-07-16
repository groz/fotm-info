import java.nio.charset.StandardCharsets

import info.fotm.util.Compression
import org.scalatest.{Matchers, FlatSpec}
import com.twitter.bijection._
import Compression._

class CompressionSpec extends FlatSpec with Matchers {

  "zip/unzip" should "receive same object when converted back and forth" in {
    val uncompressed = "Hello, world"
    val zipped = Compression.str2GZippedBase64(uncompressed)
    val unzipped = Compression.str2GZippedBase64.inverse(zipped)
    unzipped should equal (uncompressed)
  }

  "zip/unzip" should "receive same localized string when converted back and forth" in {
    val uncompressed = "Привет, мир!"
    val zipped = Compression.str2GZippedBase64(uncompressed)
    val unzipped = Compression.str2GZippedBase64.inverse(zipped)
    unzipped should equal (uncompressed)
  }

  "base64" should "encode user:pass as dXNlcjpwYXNz" in {
    val base = "user:pass"
    val base64 = Bijection[String, Base64String](base)
    base64 should equal (Base64String("dXNlcjpwYXNz"))
  }

  it should "decode dXNlcjpwYXNz as user:pass" in {
    val base64 = Base64String("dXNlcjpwYXNz")
    val decoded = Bijection.invert[String, Base64String](base64)
    decoded should equal ("user:pass")
  }

  "zip64/unzip64" should "receive same object when converted back and forth" in {
    val uncompressed = "Hello, world"
    val zipped = Bijection[String, GZippedBase64String](uncompressed)
    val unzipped = Bijection.invert[String, GZippedBase64String](zipped)
    unzipped should equal (uncompressed)
  }
}
