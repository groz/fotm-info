package info.fotm.util

import com.google.common.collect.MapMaker
import scala.collection.concurrent
import scala.collection.JavaConverters._

trait ObservableStream[T] { self =>
  type Observer = T => Unit

  protected[this] type Type = T

  // private interface
  private val subs: concurrent.Map[Subscription, Observer] = new concurrent.TrieMap[Subscription, Observer]()
  private val weaksubs: concurrent.Map[Subscription, Observer] =
    new MapMaker().concurrencyLevel(4).weakKeys.makeMap[Subscription, Observer].asScala

  private def addsub(pool: concurrent.Map[Subscription, Observer], observer: Observer): Subscription = {
    val sub = new Subscription {
      def unsubscribe(): Unit = pool -= this
    }

    pool.put(sub, observer)

    sub
  }

  private def weaksub(observer: Observer): Subscription = addsub(weaksubs, observer)

  // implementers interface
  protected def publish(value: T) = {
    for { (_, o) <- subs } o(value)
    for { (_, o) <- weaksubs } o(value)
  }

  // public interface
  def foreach(observer: Observer) = addsub(subs, observer)

  def filter(p: T => Boolean) = new ObservableStream[T] {
    val sub = self.weaksub(t => if (p(t)) publish(t))
  }

  def map[U](f: T => U) = new ObservableStream[U] {
    val sub = self.weaksub(f andThen publish)
  }

  def flatMap[U](f: T => ObservableStream[U]) = new ObservableStream[U] {
    val refs = scala.collection.mutable.Set.empty[Subscription]
    val sub = self.map(f).weaksub { refs += _.weaksub(publish) }
  }
}

trait Subscription {
  def unsubscribe(): Unit
}
