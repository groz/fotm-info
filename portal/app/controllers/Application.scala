package controllers

import javax.inject._

import akka.actor._
import akka.util.Timeout
import com.github.nscala_time.time.Imports._
import info.fotm.aether.FotmStorage._
import info.fotm.aether.{AetherConfig, MongoFotmStorage}
import info.fotm.domain.TeamSnapshot.SetupFilter
import info.fotm.domain._
import models.{DomainModels, MyBijections}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, SECONDS}

@Singleton
class Application @Inject()(system: ActorSystem) extends Controller {
  val fotmStorage = new MongoFotmStorage

  Logger.info(">>> Storage path: " + AetherConfig.dbPath)
  Logger.info(">>> Akka max message size: " + AetherConfig.config.getValue("akka.remote.netty.tcp.maximum-frame-size"))

  implicit val timeout: Timeout = new Timeout(Duration(30, SECONDS))

  def healthCheck = Action {
    Ok("OK")
  }

  def default = Action {
    Redirect(routes.Application.leaderboards("eu", "3v3"))
  }

  def teamHistory(region: String, bracket: String, teamId: String) = Action.async {

    Axis.parse(region, bracket).fold(Future.successful(NotFound: Result)) { axis =>
      val team: Team = MyBijections.teamIdBijection.inverse(teamId)

      fotmStorage.queryTeamHistory(axis, team).map { response =>
        Ok(views.html.teamHistory(response.axis, response.team, response.history))
      }
    }
  }

  def charHistory(region: String, bracket: String, charId: String) = Action.async {

    Axis.parse(region, bracket).fold(Future.successful(NotFound: Result)) { axis =>
      val id: CharacterId = MyBijections.charIdBijection.inverse(charId)
      fotmStorage.queryCharHistory(axis, id).map { response =>
        Ok(views.html.charHistory(response.axis, response.charId, response.lastSnapshot, response.history))
      }
    }
  }

  def parseFilter(filters: String): SetupFilter =
    if (filters.isEmpty) Seq.empty
    else
      for {
        filterString <- filters.split(',')
        Array(inputClassId, inputSpecId) = filterString.split('-').map(_.toInt)
        specId = if (inputSpecId == 0) None else Some(inputSpecId)
        classId = specId.fold(inputClassId)(DomainModels.specsToClass)
        if classId != 0
      } yield (classId, specId)

  def leaderboards(region: String, bracket: String, minutes: Int = 1440, perpage: Int = 20, page: Int = 1, filters: String = "") =
    Action.async {

      Logger.debug(s"Filters string: $filters")

      Axis.parse(region, bracket).fold(Future.successful(NotFound: Result)) { axis =>

        // filters format: "classId-specId"
        val setupFilter: SetupFilter = parseFilter(filters)

        Logger.debug(s"Setup filter: $setupFilter")

        val interval = new Interval(DateTime.now - minutes.minutes, DateTime.now)

        val nSetups = 20

        fotmStorage.queryAll(axis, interval, setupFilter, minutes <= 30).map { (response: QueryAllResponse) =>
          val totalPages = Math.ceil(response.teams.size / perpage.toDouble).toInt
          val teams = response.teams.slice(perpage * (page - 1), perpage * page) // 1-based indexing
          Ok(views.html.leaderboards(response.axis, response.setups.take(nSetups), teams, response.chars, minutes, perpage, page, totalPages, setupFilter))
        }
      }
    }

}
