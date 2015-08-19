package info.fotm.crawler

import akka.actor._
import dispatch.Http
import info.fotm.aether.{AetherConfig, Storage}
import info.fotm.api.{BattleNetAPISettings, BattleNetAPI}
import info.fotm.api.models._
import info.fotm.domain._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object CrawlerApp extends App {
  val apiKey = "vntnwpsguf4pqak7e8y7tgn35795fqfj"
  val system = ActorSystem(AetherConfig.crawlerSystemPath.name, AetherConfig.crawlerConfig)

  val storage = system.actorOf(Storage.props, AetherConfig.storageActorName)

  // proxy to announce to
  val storageProxy: ActorSelection = system.actorSelection(AetherConfig.storageProxyPath)
  storageProxy.tell(Storage.Announce, storage)

  val crawlInterval = 20.seconds

  val bnetSettings = BattleNetAPISettings.default.copy(timeoutInMs = crawlInterval.toMillis.toInt)

  val actorSetups = for {
    a <- Axis.all
  } yield {
      val name = s"crawler-${a.region}-${a.bracket.slug}"
      val bnetapi = new BattleNetAPI(a.region, apiKey, settings = bnetSettings).WoW
      def fetch = () => bnetapi.leaderboard(a.bracket)
      val props = Props(classOf[CrawlerActor], storage, fetch, a)
      (name, props)
    }

  def spawnAll(system: ActorSystem): List[Cancellable] =
    for {
      (name, props) <- actorSetups
    } yield {
      val crawler = system.actorOf(props, name)
      system.scheduler.schedule(0.seconds, crawlInterval, crawler, CrawlerActor.Crawl)
    }

  val timers = spawnAll(system)
}
