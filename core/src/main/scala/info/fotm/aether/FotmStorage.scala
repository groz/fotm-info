package info.fotm.aether

import com.github.nscala_time.time.Imports._
import info.fotm.aether.Storage.{QueryCharHistoryResponse, QueryTeamHistoryResponse, Updates, QueryAllResponse}
import info.fotm.domain.TeamSnapshot._
import info.fotm.domain._

import scala.concurrent.Future

trait FotmStorage {

  def update(updates: Updates): Unit

  def queryTeamHistory(axis: Axis, team: Team): Future[QueryTeamHistoryResponse]

  def queryCharHistory(axis: Axis, id: CharacterId): Future[QueryCharHistoryResponse]

  def queryAll(axis: Axis, interval: Interval, filter: SetupFilter, showChars: Boolean): Future[QueryAllResponse]

}
