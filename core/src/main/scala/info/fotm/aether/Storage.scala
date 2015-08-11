package info.fotm.aether

import akka.actor.{Props, Actor, ActorIdentity, ActorRef}
import akka.event.{Logging, LoggingReceive}
import com.github.nscala_time.time.Imports._
import com.twitter.bijection.Bijection
import info.fotm.aether.Storage.PersistedStorageState
import info.fotm.domain._
import info.fotm.util._
import play.api.libs.json.Json

import scala.collection.breakOut
import scala.collection.immutable.TreeMap

object Storage {
  val props: Props = Props[Storage]

  implicit val serializer: Bijection[PersistedStorageState, Array[Byte]] = {
    import JsonFormatters._
    implicit val pssFmt = Json.format[PersistedAxisState]

    Bijection.build[PersistedStorageState, String] { obj =>
      Json.toJson(obj).toString()
    } { str =>
      Json.parse(str).as[PersistedStorageState]
    } andThen
      Compression.str2GZippedBase64 andThen
      Compression.str2rawGZipBase64.inverse andThen
      Compression.str2bytes
  }

  val identifyMsgId = "storage"
  val Identify = akka.actor.Identify(identifyMsgId)

  type PersistedStorageState = Seq[(Axis, PersistedAxisState)]

  // input
  final case class Updates(axis: Axis, teamUpdates: Seq[TeamUpdate], charUpdates: Set[CharacterDiff]) {
    override val toString = s"Updates($axis, teams: ${teamUpdates.size}, chars: ${charUpdates.size})"
  }

  // output
  final case class QueryState(axis: Axis, interval: Interval)

  final case class QueryStateResponse(axis: Axis, teams: Seq[TeamSnapshot], chars: Seq[CharacterSnapshot])

  // reactive, subscribes/unsubscribes sender to updates
  case object Subscribe

  case object Unsubscribe

  // I'm online (again?)!
  case object Announce

}

case class StorageAxisState(
                             teams: Map[Team, TeamSnapshot] = Map.empty,
                             chars: Map[CharacterId, CharacterSnapshot] = Map.empty,
                             teamsSeen: TreeMap[DateTime, Set[Team]] = TreeMap.empty,
                             charsSeen: TreeMap[DateTime, Set[CharacterId]] = TreeMap.empty
                             )

// TODO: add JSON formatter for active state to avoid the need for Persisted/Active differentiation

case class PersistedAxisState(
                               teams: Set[TeamSnapshot],
                               chars: Set[CharacterSnapshot],
                               teamsSeen: Seq[(DateTime, Set[Team])],
                               charsSeen: Seq[(DateTime, Set[CharacterId])]
                               ) {
  def toActiveState: StorageAxisState = StorageAxisState(
    teams.map(t => (t.team, t)).toMap,
    chars.map(c => (c.id, c)).toMap,
    TreeMap.empty[DateTime, Set[Team]] ++ teamsSeen,
    TreeMap.empty[DateTime, Set[CharacterId]] ++ charsSeen
  )
}

object PersistedAxisState {
  def fromActiveState(activeState: StorageAxisState) = {
    val teams = activeState.teams.values.toSet
    val chars = activeState.chars.values.toSet
    val teamsSeen = activeState.teamsSeen.toSeq
    val charsSeen = activeState.charsSeen.toSeq
    PersistedAxisState(teams, chars, teamsSeen, charsSeen)
  }
}

class Storage(persistence: Persisted[PersistedStorageState]) extends Actor {
  import Storage._

  def this() = this(AetherConfig.storagePersistence[PersistedStorageState](Storage.serializer))

  val log = Logging(context.system, this.getClass)

  override def receive: Receive = {
    val initState: Map[Axis, StorageAxisState] = persistence.fetch().fold {
      Axis.all.map(a => (a, StorageAxisState())).toMap
    } { state => (
      for ((axis, persistedState) <- state)
        yield (axis, persistedState.toActiveState)
      )(breakOut)
    }
    process(initState, Set.empty)
  }

  def process(state: Map[Axis, StorageAxisState], subs: Set[ActorRef]): Receive = LoggingReceive {

    case msg@Updates(axis, teamUpdates: Seq[TeamUpdate], charUpdates) =>
      log.debug("Updates received. Processing...")
      val updateTime: DateTime = DateTime.now

      val currentAxis = state(axis)

      val updatedTeamSnapshots: Seq[TeamSnapshot] = for {
        update: TeamUpdate <- teamUpdates
        team = Team(update.view.snapshots.map(_.id))
        snapshotOption = currentAxis.teams.get(team)
      } yield {
          val snapshot = snapshotOption.fold {
            TeamSnapshot.fromSnapshots(update.view.snapshots)
          } {
            _.copy(view = update.view)
          }
          if (update.won) snapshot.copy(stats = snapshot.stats.win)
          else snapshot.copy(stats = snapshot.stats.loss)
        }

      val updatedTeamsState = currentAxis.teams ++ updatedTeamSnapshots.map(ts => (ts.team, ts))
      val updatedCharsState = currentAxis.chars ++ charUpdates.map(cu => (cu.id, cu.current))

      val teamsSeenThisTurn = teamUpdates.map(update => Team(update.view.snapshots.map(_.id))).toSet

      val updatedTeamsSeenState = currentAxis.teamsSeen + ((updateTime, teamsSeenThisTurn))
      val updatedCharsSeenState = currentAxis.charsSeen + ((updateTime, charUpdates.map(_.id)))

      val updatedState = StorageAxisState(updatedTeamsState, updatedCharsState, updatedTeamsSeenState, updatedCharsSeenState)

      persistence.save {
        (
          for ((axis, activeState) <- state)
            yield (axis, PersistedAxisState.fromActiveState(activeState))
          )(breakOut)
      }

      context.become(process(state.updated(axis, updatedState), subs))

      for (sub <- subs)
        sub ! msg

    case QueryState(axis: Axis, interval: Interval) =>
      val currentAxis: StorageAxisState = state(axis)
      val teamIds: Set[Team] = currentAxis.teamsSeen.from(interval.start).until(interval.end + 1.second).values.flatten.toSet
      val teams: Seq[TeamSnapshot] = teamIds.map(currentAxis.teams).toSeq

      val charIds: Set[CharacterId] = currentAxis.charsSeen.from(interval.start).until(interval.end + 1.second).values.flatten.toSet

      // filter out chars seen in teams that are sent back
      val charsInTeams: Set[CharacterId] = teamIds.flatMap(_.members)
      val chars: Seq[CharacterSnapshot] = (charIds diff charsInTeams).map(currentAxis.chars).toSeq

      sender ! QueryStateResponse(axis, teams.sortBy(-_.rating), chars.sortBy(-_.stats.rating))

    case Subscribe =>
      context.become(process(state, subs + sender))

    case Unsubscribe =>
      context.become(process(state, subs - sender))

    case Announce =>
      sender ! Subscribe

    case ActorIdentity(correlationId, storageRefOpt) if correlationId == identifyMsgId =>
      log.debug(s"Storage actor identity: $storageRefOpt")
      storageRefOpt.foreach { storageRef => storageRef ! Subscribe }
  }
}
