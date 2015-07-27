package info.fotm.aether

import akka.actor.{Actor, ActorRef}
import akka.event.{Logging, LoggingReceive}
import com.github.nscala_time.time.Imports._
import info.fotm.aether.Storage.PersistedStorageState
import info.fotm.domain._
import info.fotm.util.{NullPersisted, Persisted}

import scala.collection.breakOut
import scala.collection.immutable.TreeMap

object Storage {
  type PersistedStorageState = Seq[(Axis, PersistedAxisState)]

  // init flows
  final case class Init(state: Map[Axis, StorageAxisState])

  case object InitFrom

  // input
  final case class Updates(axis: Axis, teamUpdates: Seq[TeamUpdate], charUpdates: Set[CharacterDiff])

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
  teams    : Map[Team, TeamSnapshot] = Map.empty,
  chars    : Map[CharacterId, CharacterSnapshot] = Map.empty,
  teamsSeen: TreeMap[DateTime, Set[Team]] = TreeMap.empty,
  charsSeen: TreeMap[DateTime, Set[CharacterId]] = TreeMap.empty
)

// TODO: add JSON formatter for active state to avoid the need for Persisted/Active differentiation

case class PersistedAxisState(
  teams    : Set[TeamSnapshot],
  chars    : Set[CharacterSnapshot],
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

class Storage(persistanceOpt: Option[Persisted[PersistedStorageState]] = None) extends Actor {
  import Storage._

  val persistance = persistanceOpt.getOrElse(new NullPersisted[PersistedStorageState])

  val log = Logging(context.system, this.getClass)

  override def receive: Receive = {
    val initState: Map[Axis, StorageAxisState] = persistance.fetch().fold {
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

      persistance.save { (
          for ((axis, activeState) <- state)
          yield (axis, PersistedAxisState.fromActiveState(activeState))
        )(breakOut)
      }

      context.become(process(state.updated(axis, updatedState), subs))

      for (sub <- subs)
        sub ! msg

    case QueryState(axis: Axis, interval: Interval) =>
      val currentAxis = state(axis)
      val teamIds = currentAxis.teamsSeen.from(interval.start).until(interval.end + 1.second).values.flatten.toSet
      val teams = teamIds.map(currentAxis.teams).toSeq
      val charIds = currentAxis.charsSeen.from(interval.start).until(interval.end + 1.second).values.flatten.toSet
      val chars = charIds.map(currentAxis.chars).toSeq

      sender ! QueryStateResponse(axis, teams, chars)

    case Init(initState) =>
      context.become(process(initState, subs))

    case InitFrom =>
      sender ! Init(state)

    case Subscribe =>
      context.become(process(state, subs + sender))

    case Unsubscribe =>
      context.become(process(state, subs - sender))

    case Announce =>
      sender ! InitFrom
      sender ! Subscribe
  }

}
