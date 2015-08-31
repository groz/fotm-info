package info.fotm.aether

import akka.util.Timeout
import info.fotm.domain._
import akka.actor._
import akka.pattern.ask
import com.github.nscala_time.time.Imports._

import scala.concurrent.Await


object TempApp extends App {

  val system = ActorSystem(AetherConfig.crawlerSystemPath.name, AetherConfig.crawlerConfig)
  val storage = system.actorOf(Storage.props, AetherConfig.storageActorName)

  implicit val timeout: Timeout = new Timeout(scala.concurrent.duration.Duration(30, scala.concurrent.duration.SECONDS))

  def stopwatch(action: => Unit) = {
    val startTime = DateTime.now
    action
    val interval = new Interval(startTime, DateTime.now).toDurationMillis
    println(interval)
  }

  def query(regionSlug: String, bracketSlug: String, nMinutes: Int) = {
    val interval = new Interval(DateTime.now - nMinutes.minutes, DateTime.now)
    val request = storage ? Storage.QueryAll(Axis.parse(regionSlug, bracketSlug).get, interval, Seq.empty)
    Await.result(request, scala.concurrent.duration.Duration.Inf)
  }

  /*
import info.fotm.aether.TempApp._
main(Array.empty)
stopwatch { query("us", "3v3", 15000) }
   */
}
