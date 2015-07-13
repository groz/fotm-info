package info.fotm.crawler

import akka.actor.{Props, ActorSystem}
import dispatch.Http
import info.fotm.api.BattleNetAPI
import info.fotm.domain.Axis
import scala.concurrent.Await
import scala.concurrent.duration._
import info.fotm.api.models._
import info.fotm.api._

import scala.concurrent.ExecutionContext.Implicits.global

object CrawlerApp extends App {
  val apiKey = "vntnwpsguf4pqak7e8y7tgn35795fqfj"

  val regions = List(Europe)
  val brackets = List(Twos, Threes)

  val actorSetups = for {
    region <- regions
    bracket <- brackets
  } yield {
      val name = s"crawler-$region-${bracket.slug}"
      val props = Props(classOf[CrawlerActor], apiKey, Axis(region, bracket))
      (name, props)
    }

  def spawnAll(system: ActorSystem): Unit =
    for ((name, props) <- actorSetups) {
      val crawler = system.actorOf(props, name)
      system.scheduler.schedule(0 seconds, 10 seconds, crawler, CrawlerActor.Crawl)
    }

  def run() = spawnAll( ActorSystem("crawlerSystem") )
}

object MyApp extends App {
  val apiKey = "vntnwpsguf4pqak7e8y7tgn35795fqfj"

  val api = new BattleNetAPI(US, apiKey).WoW

  val lbFuture = api.leaderboard(Threes).map { lb =>
    val gs = lb.rows.groupBy(_.specId).map { kv =>
      val (spec, g) = kv
      (spec, g.size)
    }
    gs.toList.sortBy(-_._2).foreach(println)
  }

  Await.result(lbFuture, Duration.Inf)
  Http.shutdown()
}
