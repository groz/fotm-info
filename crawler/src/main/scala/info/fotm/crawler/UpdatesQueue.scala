package info.fotm.crawler

import akka.event.{NoLogging, LoggingAdapter}
import info.fotm.util.ObservableStream
import scala.collection.mutable

class UpdatesQueue[T](maxSize: Int = -1)(implicit ordering: Ordering[T], log: LoggingAdapter = NoLogging) {
  val history = mutable.TreeSet.empty

  /*
  Turned out to be pretty useless abstraction for this particular case, but left here for now.
   */
  val stream = new ObservableStream[(T, T)] {
    def signal(value: Type) = super.publish(value)
  }

  def process(current: T): Unit = {
    if (history.add(current)) {
      val before = history.until(current)
      val after = history.from(current).tail

      val strBefore = before.toIndexedSeq.map(_ => "_").mkString
      val strAfter = after.toIndexedSeq.map(_ => "_").mkString

      val prev: Option[T] = before.lastOption
      prev.foreach { p =>
        stream.signal(p, current)
      }

      val next = history.from(current).tail.headOption
      next.foreach { n =>
        stream.signal(current, n)
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
