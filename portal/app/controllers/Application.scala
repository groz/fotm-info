package controllers

import javax.inject._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.github.nscala_time.time.Imports
import info.fotm.aether.{AetherConfig, Storage}
import info.fotm.domain.TeamSnapshot.SetupFilter
import info.fotm.domain.{Team, CharacterId, Axis}
import com.github.nscala_time.time.Imports._
import models.DomainModels
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

  def default = Action {
    Redirect(routes.Application.leaderboards("eu", "3v3"))
  }

  def teamHistory(region: String, bracket: String, teamId: String) = Action.async {

    Axis.parse(region, bracket).fold(Future.successful(NotFound: Result)) { axis =>
      val team: Team = Storage.teamIdBijection.inverse(teamId)
      val request = storageProxy ? Storage.QueryTeamHistory(axis, team)
      request.mapTo[Storage.QueryTeamHistoryResponse].map { response =>
        Ok(views.html.teamHistory(response.axis, response.team, response.history))
      }
    }
  }

  def charHistory(region: String, bracket: String, charId: String) = Action.async {

    Axis.parse(region, bracket).fold(Future.successful(NotFound: Result)) { axis =>
      val id: CharacterId = Storage.charIdBijection.inverse(charId)
      val request = storageProxy ? Storage.QueryCharHistory(axis, id)
      request.mapTo[Storage.QueryCharHistoryResponse].map { response =>
        Ok(views.html.charHistory(response.axis, response.charId, response.lastSnapshot, response.history))
      }
    }
  }

  def leaderboards(region: String, bracket: String, minutes: Int = 1440, perpage: Int = 20, page: Int = 1, filters: String = "") =
    Action.async {

      Logger.debug(s"Filters string: $filters")

      Axis.parse(region, bracket).fold(Future.successful(NotFound: Result)) { axis =>

        // filters format: "classId-specId"
        val setupFilter: SetupFilter =
          if (filters.isEmpty) Seq.empty
          else
            for {
              filterString <- filters.split(',')
              Array(inputClassId, inputSpecId) = filterString.split('-').map(_.toInt)
              specId = if (inputSpecId == 0) None else Some(inputSpecId)
              classId = specId.fold(inputClassId)(DomainModels.specsToClass)
              if classId != 0
            } yield (classId, specId)

        Logger.debug(s"Setup filter: $setupFilter")

        val interval = new Interval(DateTime.now - minutes.minutes, DateTime.now)
        val request = storageProxy ? Storage.QueryAll(axis, interval, setupFilter)

        val nSetups = 20

        request.mapTo[Storage.QueryAllResponse].map { response =>
          val totalPages = Math.ceil(response.teams.size / perpage.toDouble).toInt
          val teams = response.teams.slice(perpage * (page - 1), perpage * page) // 1-based indexing
          Ok(views.html.leaderboards(response.axis, response.setups.take(nSetups), teams, response.chars, minutes, perpage, page, totalPages, setupFilter))
        }
      }
    }

}
