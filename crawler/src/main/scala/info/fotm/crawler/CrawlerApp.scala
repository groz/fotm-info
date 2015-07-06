package info.fotm.crawler

import akka.actor.{Props, ActorSystem}
import info.fotm.api.models._
import info.fotm.api.regions._
import info.fotm.crawler.CrawlerActor.Crawl

import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

object CrawlerApp extends App {
  val apiKey = "vntnwpsguf4pqak7e8y7tgn35795fqfj"

  val system = ActorSystem("crawlerSystem")

  val regions = List(US, Europe, Korea, Taiwan, China)
  val brackets = List(Twos, Threes, Fives, Rbg)

  for {
    region <- regions
    bracket <- brackets
  } {
    val name = s"crawler-$region-${bracket.slug}"
    val crawler = system.actorOf(Props(classOf[CrawlerActor], apiKey, region, bracket), name)
    //system.scheduler.schedule(0 seconds, 10 seconds, crawler, Crawl)
  }
}