package info.fotm.crawler

import scala.collection.mutable
import akka.event.slf4j.Logger





class ConsecutiveUpdatesObserver[T](maxSize: Int = -1)
                        (implicit ordering: Ordering[T]) extends ObservableStream[(T, T)] {
  val log = Logger(this.getClass.getName)
  val history = mutable.TreeSet.empty

  def process(current: T): Unit = {
    if (history.add(current)) {
      val before = history.until(current)
      val after = history.from(current).tail

      val strBefore = before.toIndexedSeq.map(_ => "_").mkString
      val strAfter = after.toIndexedSeq.map(_ => "_").mkString

      val prev: Option[T] = before.lastOption
      prev.foreach { p =>
        publish(p, current)
      }

      val next = history.from(current).tail.headOption
      next.foreach { n =>
        publish(current, n)
      }

      if (maxSize != -1 && history.size > maxSize) {
        history -= history.head
      }

      log.debug(s"History queue: ${strBefore}X${strAfter}")
    } else {
      log.debug("Update already in history.")
    }
  }
}
