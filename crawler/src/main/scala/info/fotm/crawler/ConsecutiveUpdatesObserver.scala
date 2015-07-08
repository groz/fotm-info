package info.fotm.crawler

import scala.collection.mutable

class ConsecutiveUpdatesObserver[T](onUpdate: (T, T) => Unit, maxSize: Int = -1, filter: (T, T) => Boolean = (a: T, b: T) => true)
                        (implicit ordering: Ordering[T]) {
  val history = mutable.TreeSet.empty

  def process(current: T): String = {
    if (history.add(current)) {
      val before = history.until(current)
      val after = history.from(current).tail

      val prev: Option[T] = before.lastOption
      prev.filter(filter(_, current)).foreach {
        onUpdate(_, current)
      }

      val next = history.from(current).tail.headOption
      next.filter(filter(current, _)).foreach {
        onUpdate(current, _)
      }

      if (maxSize != -1 && history.size > maxSize) {
        history -= history.head
      }

      val strBefore = before.toIndexedSeq.map(_ => "_").mkString
      val strAfter = after.toIndexedSeq.map(_ => "_").mkString
      s"History queue: ${strBefore}X${strAfter}"
    } else ""
  }
}
