package info.fotm.aether

import com.github.nscala_time.time.Imports._
import info.fotm.domain.TeamSnapshot._
import info.fotm.domain._

import scala.collection.immutable.TreeMap
import scala.concurrent.Future

trait FotmStorage {
  import FotmStorage._
  def update(updates: Updates): Unit
  def queryTeamHistory(axis: Axis, team: Team): Future[QueryTeamHistoryResponse]
  def queryCharHistory(axis: Axis, id: CharacterId): Future[QueryCharHistoryResponse]
  def queryAll(axis: Axis, interval: Interval, filter: SetupFilter, showChars: Boolean): Future[QueryAllResponse]
}

object FotmStorage {
  final case class Updates(axis: Axis, teamUpdates: Seq[TeamUpdate], charUpdates: Set[CharacterDiff]) {
    override val toString = s"Updates($axis, teams: ${teamUpdates.size}, chars: ${charUpdates.size})"
  }

  final case class QueryAll(axis: Axis, interval: Interval, filter: SetupFilter)
  final case class QueryAllResponse(axis: Axis, setups: Seq[FotmSetup], teams: Seq[TeamSnapshot], chars: Seq[CharacterSnapshot])

  final case class QueryTeamHistory(axis: Axis, team: Team)
  final case class QueryTeamHistoryResponse(axis: Axis, team: Team, history: TreeMap[DateTime, TeamSnapshot])

  final case class QueryCharHistory(axis: Axis, id: CharacterId)
  final case class QueryCharHistoryResponse(axis: Axis, charId: CharacterId, lastSnapshot: Option[CharacterSnapshot], history: TreeMap[DateTime, TeamSnapshot])

  final case class QueryFotm(axis: Axis, interval: Interval)
  final case class QueryFotmResponse(axis: Axis, setups: Seq[FotmSetup])
}
