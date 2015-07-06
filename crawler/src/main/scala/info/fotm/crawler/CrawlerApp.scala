package info.fotm.crawler

import akka.actor.{Props, ActorSystem}
import info.fotm.api.models.Threes
import info.fotm.api.regions.US
import info.fotm.crawler.CrawlerActor.Crawl

import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

object CrawlerApp extends App {
  val apiKey = "vntnwpsguf4pqak7e8y7tgn35795fqfj"

  val system = ActorSystem("crawlerSystem")
  val crawler = system.actorOf(Props(classOf[CrawlerActor], apiKey, US, Threes), "crawler-US-3v3")

  system.scheduler.schedule(0 seconds, 10 seconds, crawler, Crawl)
}
