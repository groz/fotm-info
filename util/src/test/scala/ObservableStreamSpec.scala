import info.fotm.util.ObservableStream
import org.scalatest.{Matchers, FlatSpec}

class ObservableStreamSpec extends FlatSpec with Matchers {

  "publish" should "should notify subscribers of correct value" in {
    val s = new ObservableStream[Int] {}

    var published: Int = 0
    s.foreach { published = _ }

    s.publish(10)
    published should equal(10)

    s.publish(20)
    published should equal(20)
  }

  it should "should not notify unsubscribed subs" in {
    val s = new ObservableStream[Int] {}

    var published: Int = 0
    val sub = s.foreach { published = _ }

    s.publish(10)
    published should equal(10)
    sub.unsubscribe()

    s.publish(20)
    published should equal(10)
  }

  "map" should "map published value for subs" in {
    val s = new ObservableStream[Int] {}

    var published: Int = 0

    val sub = s.map(_ * 2).foreach { published = _ }

    s.publish(10)
    published should equal(20)
  }

  it should "not fire when unsubscribed" in {
    val s = new ObservableStream[Int] {}

    var published: Int = 0

    val sub = s.map(_ * 2).foreach { published = _ }
    sub.unsubscribe()

    s.publish(10)
    published should equal(0)
  }

  "filter" should "not fire for skipped results" in {
    val s = new ObservableStream[Int] {}

    var published: Int = 0

    val sub = s.filter(_ % 2 == 0).foreach { published = _ }

    s.publish(9)
    published should equal(0)
  }

  it should "fire for filtered results" in {
    val s = new ObservableStream[Int] {}

    var published: Int = 0

    val sub = s.filter(_ % 2 == 0).foreach { published = _ }

    s.publish(10)
    published should equal(10)
  }

  "flatMap" should "correctly fire" in {
    val s1 = new ObservableStream[Int] {}
    val s2 = new ObservableStream[Int] {}

    var published: (Int, Int) = (0, 0)

    val s12 = for {
      x <- s1
      y <- s2
    } yield (x, y)

    s12.foreach(published = _)

    s1.publish(10)
    s2.publish(20)

    published should equal((10, 20))
  }

}
