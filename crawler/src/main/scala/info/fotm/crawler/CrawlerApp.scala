package info.fotm.crawler

import akka.actor._
import info.fotm.aether.AetherConfig
import info.fotm.api.{BattleNetAPI, BattleNetAPISettings}
import info.fotm.domain._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object CrawlerApp extends App {
  val apiKey = "vntnwpsguf4pqak7e8y7tgn35795fqfj"
  val system = ActorSystem(AetherConfig.crawlerSystemPath.name, AetherConfig.crawlerConfig)

  val crawlInterval = 20.seconds

  val bnetSettings = BattleNetAPISettings.default.copy(timeoutInMs = crawlInterval.toMillis.toInt)

  val actorSetups = for {
    a <- Axis.all
  } yield {
      val name = s"crawler-${a.region}-${a.bracket.slug}"
      val bnetapi = new BattleNetAPI(a.region, apiKey, settings = bnetSettings).WoW
      def fetch = () => bnetapi.leaderboard(a.bracket)
      val props = Props(classOf[CrawlerActor], fetch, a)
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
