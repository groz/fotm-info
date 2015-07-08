import info.fotm.crawler.ConsecutiveUpdatesObserver
import org.scalatest._

class ConsecutiveUpdatesObserverSpec extends FlatSpec with Matchers {

  "process" should "not fire for single elements" in {
    var fired = false
    val updates = new ConsecutiveUpdatesObserver[Int]((_, _) => fired = true)
    updates.process(1)
    fired should be(false)
  }

  it should "fire for simple pairs" in {
    var fired = false
    val updates = new ConsecutiveUpdatesObserver[Int]((_, _) => fired = true)

    updates.process(1)
    fired should be(false)

    updates.process(2)
    fired should be(true)
  }

  it should "not fire for pairs not passing filter" in {
    var fired = false
    val updates = new ConsecutiveUpdatesObserver[Int]((_, _) => fired = true, filter = (a, b) => a > b)

    updates.process(1)
    fired should be(false)

    updates.process(2)
    fired should be(false)
  }

  it should "fire for pairs passing filter" in {
    var fired = false
    val updates = new ConsecutiveUpdatesObserver[Int]((_, _) => fired = true, filter = (a, b) => a < b)

    updates.process(2)
    fired should be(false)

    updates.process(0)
    fired should be(true)
  }

  it should "not fire for non consecutive updates" in {
    var fired = false
    val updates = new ConsecutiveUpdatesObserver[Int]((_, _) => fired = true)

    updates.process(1)
    fired should be(false)

    updates.process(3)
    fired should be(true)
  }
}
