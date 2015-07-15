package controllers

import akka.actor.Actor.Receive
import akka.util.Timeout
import info.fotm.aether.Storage
import info.fotm.aether.Storage.TeamLadderResponse
import info.fotm.domain.Axis
import models._

import javax.inject._
import akka.actor._
import play.api._
import play.api.mvc._

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern.ask

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
      val request = storageProxy ? Storage.GetTeamLadder(axis)
      request.mapTo[Storage.TeamLadderResponse].map { (response: TeamLadderResponse) =>
        Ok(views.html.index("Teams", response.teamLadder))
      }
    }

  }

}
