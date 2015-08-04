package controllers

import javax.inject._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.github.nscala_time.time.Imports
import info.fotm.aether.{AetherRoutes, Storage}
import info.fotm.domain.Axis
import com.github.nscala_time.time.Imports._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, SECONDS}

@Singleton
class Application @Inject()(system: ActorSystem) extends Controller {

  Logger.info("*** Storage path: " + AetherRoutes.storagePath)
  Logger.info("*** Proxy path: " + AetherRoutes.storageProxyPath)

  implicit val timeout: Timeout = new Timeout(Duration(30, SECONDS))

  def interval: Imports.Interval = new Interval(DateTime.now - 1.month, DateTime.now)

  // init proxy and subscribe to storage updates
  lazy val storage: ActorSelection = system.actorSelection(AetherRoutes.storagePath)
  lazy val storageProxy = system.actorOf(Props(classOf[Storage], None), AetherRoutes.storageProxyActorName)
  storage.tell(Storage.Identify, storageProxy)

  Logger.info("*** StorageProxy instantiated at " + storageProxy.path)

  def healthCheck = Action {
    Logger.info("**** HEALTHCHECK LOGGED ****")
    Ok("healthy")
  }

  def index(region: String, bracket: String): Action[AnyContent] = Action.async {
    Logger.info("**** INDEX LOGGED ****")

    Axis.parse(region, bracket).fold(Future.successful(NotFound: Result)) { axis =>
      val request = storageProxy ? Storage.QueryState(axis, interval)

      request.mapTo[Storage.QueryStateResponse].map { (response: Storage.QueryStateResponse) =>
        Ok(views.html.index("Playing Now", response.axis, response.teams, response.chars))
      }
    }
  }

}
