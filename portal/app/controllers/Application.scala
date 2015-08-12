package controllers

import javax.inject._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.github.nscala_time.time.Imports
import info.fotm.aether.{AetherConfig, Storage}
import info.fotm.domain.Axis
import com.github.nscala_time.time.Imports._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, SECONDS}

@Singleton
class Application @Inject()(system: ActorSystem) extends Controller {

  Logger.info(">>> Storage path: " + AetherConfig.storagePath)
  Logger.info(">>> Proxy path: " + AetherConfig.storageProxyPath)

  implicit val timeout: Timeout = new Timeout(Duration(30, SECONDS))

  // init proxy and subscribe to storage updates
  lazy val storage: ActorSelection = system.actorSelection(AetherConfig.storagePath)
  lazy val storageProxy = system.actorOf(Storage.readonlyProps, AetherConfig.storageProxyActorName)
  storage.tell(Storage.Identify, storageProxy)

  def healthCheck = Action { Ok("OK") }

  def playingNowDefault = playingNowRegion("eu")

  def playingNowRegion(region: String) = playingNow30(region, "3v3")

  def playingNow30(region: String, bracket: String) = playingNow(region, bracket, 30)

  def playingNow(region: String, bracket: String, nMinutes: Int): Action[AnyContent] = Action.async {

    Axis.parse(region, bracket).fold(Future.successful(NotFound: Result)) { axis =>

      val interval = new Interval(DateTime.now - nMinutes.minutes, DateTime.now)
      val request = storageProxy ? Storage.QueryState(axis, interval)

      request.mapTo[Storage.QueryStateResponse].map { (response: Storage.QueryStateResponse) =>
        Ok(views.html.playingNow(response.axis, response.teams, response.chars, nMinutes))
      }
    }
  }

}
