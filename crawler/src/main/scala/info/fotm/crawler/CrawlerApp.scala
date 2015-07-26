package info.fotm.crawler

import akka.actor._
import dispatch.Http
import info.fotm.aether.Storage.PersistedStorageState
import info.fotm.aether.{PersistedAxisState, FilePersisted, Storage}
import info.fotm.api.BattleNetAPI
import info.fotm.api.models._
import info.fotm.domain._
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object CrawlerApp extends App {
  val apiKey = "vntnwpsguf4pqak7e8y7tgn35795fqfj"

  val system = ActorSystem("crawlerSystem")

  val filePersisted = {
    import JsonFormatters._
    implicit val pssFmt = Json.format[PersistedAxisState]
    new FilePersisted[PersistedStorageState]("storage.txt")
  }

  val storage = system.actorOf(Props(classOf[Storage], Some(filePersisted)), "storage")

  // proxy to announce to
  val storageProxy: ActorSelection =
    system.actorSelection("akka.tcp://application@127.0.0.1:33200/user/storage-proxy")

  storageProxy.tell(Storage.Announce, storage)

  val actorSetups = for {
    a <- Axis.all
  } yield {
      val name = s"crawler-${a.region}-${a.bracket.slug}"
      val props = Props(classOf[CrawlerActor], storage, apiKey, a)
      (name, props)
    }

  def spawnAll(system: ActorSystem): List[Cancellable] =
    for {
      (name, props) <- actorSetups
    } yield {
      val crawler = system.actorOf(props, name)
      system.scheduler.schedule(0.seconds, 10.seconds, crawler, CrawlerActor.Crawl)
    }

  val timers = spawnAll(system)
}

object MyApp extends App {
  val apiKey = "vntnwpsguf4pqak7e8y7tgn35795fqfj"

  val api = new BattleNetAPI(US, apiKey).WoW

  val lbFuture = api.leaderboard(Threes).map { lb =>
    val gs = for {(specId, g) <- lb.rows.groupBy(_.specId)} yield (specId, g.size)
    gs.toList.sortBy(-_._2).foreach(println)
  }

  Await.result(lbFuture, Duration.Inf)
  Http.shutdown()
}
