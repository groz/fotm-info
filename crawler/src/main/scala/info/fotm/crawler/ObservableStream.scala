package info.fotm.crawler

import com.google.common.collect.MapMaker
import scala.collection.JavaConverters._
import scala.collection.concurrent.Map

trait ObservableStream[T] { self =>
  type Observer = T => Unit

  val observers: Map[Subscription, Observer] =
    new MapMaker().concurrencyLevel(4).weakKeys.makeMap[Subscription, Observer].asScala

  def subscribe(observer: Observer): Subscription = {
    val sub = new Subscription {
      def unsubscribe(): Unit = observers -= this
    }

    observers.put(sub, observer)

    sub
  }

  def publish(value: T) = for { (_, o) <- observers } o(value)

  def filter(p: T => Boolean) = new ObservableStream[T] {
    self.subscribe(t => if (p(t)) this.publish(t))
  }

  def map[U](f: T => U) = new ObservableStream[U] {
    self.subscribe(t => this.publish(f(t)))
  }

  def flatMap[U](f: T => ObservableStream[U]) = new ObservableStream[U] {
    self.subscribe(t => f(t).subscribe(u => this.publish(u)))
  }
}

trait Subscription {
  def unsubscribe(): Unit
}
