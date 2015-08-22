import info.fotm.util.{Compression, FilePersisted}
import org.scalatest.{Matchers, FlatSpec}

class FilePersistedSpec extends FlatSpec with Matchers {

  implicit val serializer = Compression.str2bytes

  "save/fetch" should "receive back the same object as saved" in {
    val text = "hello, here's a string"
    val f = new FilePersisted[String]("tmp.txt")
    f.save(text)
    f.fetch().toOption.contains(text) should be(true)
  }

  "fetch" should "get None if there's no such file" in {
    val f = new FilePersisted[String]("not_existing.txt")
    f.fetch().toOption should be(None)
  }
}
