package controllers

import javax.inject._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import info.fotm.aether.Storage
import info.fotm.domain.Axis
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class Application @Inject() (system: ActorSystem) extends Controller {

  implicit val timeout: Timeout = 30.seconds

  lazy val storage: ActorSelection =
    system.actorSelection("akka.tcp://crawlerSystem@127.0.0.1:33100/user/storage")

  // init proxy and subscribe to storage updates
  lazy val storageProxy = system.actorOf(Props[Storage], "storage-proxy")
  storage.tell(Storage.InitFrom, storageProxy)
  storage.tell(Storage.Subscribe, storageProxy)

  def index(regionSlug: String, bracketSlug: String) = Action.async {

    Axis(regionSlug, bracketSlug).fold {
      Future.successful(NotFound: Result)
    } { axis =>
      val request = storageProxy ? Storage.QueryState(axis)
      request.mapTo[Storage.QueryStateResponse].map { (response: Storage.QueryStateResponse) =>
        Ok(views.html.index("Playing Now", response.axis, response.teamLadder, response.chars))
      }
    }

  }

}
