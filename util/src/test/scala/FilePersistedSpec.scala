import info.fotm.util.Compression._
import info.fotm.util.FilePersisted
import org.scalatest.{Matchers, FlatSpec}
import com.twitter.bijection._

class FilePersistedSpec extends FlatSpec with Matchers {

  "save/fetch" should "receive back the same object as saved" in {
    val text = "hello, here's a string"
    val f = new FilePersisted("tmp.txt", str2bytes)
    f.save(text)
    f.fetch() should be(Some(text))
  }

  "fetch" should "get None if there's no such file" in {
    val f = new FilePersisted("not_existing.txt", str2bytes)
    f.fetch() should be(None)
  }
}
