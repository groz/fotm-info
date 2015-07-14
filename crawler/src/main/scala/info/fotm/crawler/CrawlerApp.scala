package info.fotm.crawler

import akka.actor.{ActorSelection, ActorRef, Props, ActorSystem}
import akka.util.Timeout
import dispatch.Http
import info.fotm.aether.Storage
import info.fotm.api.BattleNetAPI
import info.fotm.domain._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import info.fotm.api.models._
import info.fotm.api._

import scala.concurrent.ExecutionContext.Implicits.global

object CrawlerApp extends App {
  val apiKey = "vntnwpsguf4pqak7e8y7tgn35795fqfj"

  val system = ActorSystem("crawlerSystem")

  val storageSelector: ActorSelection = system.actorSelection("akka.tcp://application@127.0.0.1:45000/user/storage-actor")
  //val resolver: Future[ActorRef] = storageSelector.resolveOne()(5.seconds)
  //val storage: ActorRef = Await.result(resolver, 5.seconds)
  //storage ! Storage.Updates(Axis(US, Fives), Seq())

  val actorSetups = for {
    a <- Axis.all
  } yield {
      val name = s"crawler-${a.region}-${a.bracket.slug}"
      val props = Props(classOf[CrawlerActor], storageSelector, apiKey, a)
      (name, props)
    }

  def spawnAll(system: ActorSystem): Unit =
    for ((name, props) <- actorSetups) {
      val crawler = system.actorOf(props, name)
      system.scheduler.schedule(0 seconds, 10 seconds, crawler, CrawlerActor.Crawl)
    }

  spawnAll(system)
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
