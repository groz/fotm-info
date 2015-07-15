package info.fotm.aether

import akka.actor.Actor
import akka.actor.Actor.Receive

object Analytics {
  case class Event(name: String, value: Int)
}

class Analytics extends Actor {
  override def receive: Receive = ???
}
