package info.fotm.crawler

import akka.event.{LoggingAdapter, NoLogging}
import info.fotm.util.ObservableStream

import scala.collection.mutable

class UpdatesQueue[T](maxSize: Int = -1)(implicit ordering: Ordering[T], log: LoggingAdapter = NoLogging)
  extends ObservableStream[(T, T)] {
  val history = mutable.TreeSet.empty

  /*
  Turned out to be pretty useless abstraction for this particular case, but left here for now.
   */
  def process(current: T): Unit = {
    if (history.add(current)) {
      val before = history.until(current)
      val after = history.from(current).tail

      val strBefore = before.toIndexedSeq.map(_ => "_").mkString
      val strAfter = after.toIndexedSeq.map(_ => "_").mkString

      val prev: Option[T] = before.lastOption
      prev.foreach { p =>
        log.debug("Signaling _X")
        publish(p, current)
      }

      val next = history.from(current).tail.headOption
      next.foreach { n =>
        log.debug("Signaling X_")
        publish(current, n)
      }

      if (maxSize != -1 && history.size > maxSize) {
        history -= history.head
      }

      log.debug(s"History queue (${history.size}): ${strBefore}X${strAfter}")
    } else {
      log.debug("Update already in history.")
    }
  }
}
