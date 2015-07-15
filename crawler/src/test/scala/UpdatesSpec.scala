import akka.event.NoLogging
import info.fotm.crawler.UpdatesQueue
import org.scalatest._

class UpdatesSpec extends FlatSpec with Matchers {

  "process" should "not fire for single elements" in {
    var fired = false

    val updates = new UpdatesQueue[Int]()
    for { _ <- updates } fired = true

    updates.process(1)
    fired should be(false)
  }

  it should "fire for simple pairs" in {
    var fired = false

    val updates = new UpdatesQueue[Int]()
    for { _ <- updates } fired = true

    updates.process(1)
    fired should be(false)

    updates.process(2)
    fired should be(true)
  }

  it should "not fire for pairs not passing filter" in {
    var fired = false
    val updates = new UpdatesQueue[Int]()
    for { _ <- updates if false } fired = true

    updates.process(1)
    fired should be(false)

    updates.process(2)
    fired should be(false)
  }

  it should "fire for pairs passing filter" in {
    var fired = false
    val updates = new UpdatesQueue[Int]()
    for { (a, b) <- updates if a < b } fired = true

    updates.process(2)
    fired should be(false)

    updates.process(0)
    fired should be(true)
  }

  it should "not fire for non consecutive updates" in {
    var fired = false
    val updates = new UpdatesQueue[Int]()
    for { _ <- updates } fired = true

    updates.process(1)
    fired should be(false)

    updates.process(3)
    fired should be(true)
  }

  it should "fire twice for updates put in the middle" in {
    val pairs = scala.collection.mutable.ListBuffer[(Int, Int)]()

    val updates = new UpdatesQueue[Int]()
    for { (a, b) <- updates if a + 1 == b } pairs += ((a, b))

    val expected = List((1, 2), (2, 3))

    updates.process(3)
    updates.process(1)

    pairs.size should be (0)

    updates.process(2)

    pairs should contain theSameElementsAs(expected)
  }
}
