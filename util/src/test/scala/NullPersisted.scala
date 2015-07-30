import info.fotm.util.NullPersisted
import org.scalatest.{Matchers, FlatSpec}

class NullPersistedSpec extends FlatSpec with Matchers {

  "fetch" should "always return None" in {

    val f = new NullPersisted()
    f.fetch() should be(None)
  }
}
