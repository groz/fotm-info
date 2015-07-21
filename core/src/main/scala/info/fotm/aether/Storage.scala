package info.fotm.aether

import akka.actor.{Actor, ActorRef}
import akka.event.{Logging, LoggingReceive}
import com.github.nscala_time.time.Imports._
import info.fotm.domain._

import scala.collection.immutable.TreeMap

object Storage {
  // tables
  type Seen[T] = TreeMap[DateTime, Set[T]]
  type TeamLadderAxis = Map[Team, TeamSnapshot]
  type CharSnapshotsAxis = Map[CharacterId, CharacterSnapshot]

  // init flows
  final case class Init(ladders: Map[Axis, TeamLadderAxis],
                        chars: Map[Axis, CharSnapshotsAxis],
                        teamsSeen: Map[Axis, Seen[Team]],
                        charsSeen: Map[Axis, Seen[CharacterId]])

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

class Storage extends Actor {

  import Storage._

  val log = Logging(context.system, this.getClass)
  // TODO: save/load state on init

  override def receive: Receive = process(Map.empty, Map.empty, Map.empty, Map.empty, Set.empty)

  def process(
               ladders: Map[Axis, TeamLadderAxis],
               chars: Map[Axis, CharSnapshotsAxis],
               teamsSeen: Map[Axis, Seen[Team]],
               charsSeen: Map[Axis, Seen[CharacterId]],
               subs: Set[ActorRef])
  : Receive = LoggingReceive {

    case msg@Updates(axis, teamUpdates: Seq[TeamUpdate], charUpdates) =>
      log.debug("Updates received. Processing...")
      val updateTime: DateTime = DateTime.now

      val ladderAxis: TeamLadderAxis = ladders.getOrElse(axis, Map.empty)

      val updatedTeamSnapshots: Seq[TeamSnapshot] = for {
        update: TeamUpdate <- teamUpdates
        team = Team(update.view.snapshots.map(_.id))
        snapshotOption = ladderAxis.get(team)
      } yield {
          val snapshot = snapshotOption.fold {
            TeamSnapshot(update.view.snapshots)
          } {
            _.copy(view = update.view)
          }
          if (update.won) snapshot.copy(stats = snapshot.stats.win)
          else snapshot.copy(stats = snapshot.stats.loss)
        }

      val newLadderAxis: TeamLadderAxis = ladderAxis ++ updatedTeamSnapshots.map(ts => (ts.team, ts))

      val charsAxis: CharSnapshotsAxis = chars.getOrElse(axis, Map.empty)
      val newCharsAxis: CharSnapshotsAxis = charsAxis ++ charUpdates.map(cu => (cu.id, cu.current))

      val newLaddersState = ladders.updated(axis, newLadderAxis)
      val newCharsState = chars.updated(axis, newCharsAxis)

      val teamsSeenAxis: Seen[Team] = teamsSeen.getOrElse(axis, TreeMap.empty)
      val charsSeenAxis: Seen[CharacterId] = charsSeen.getOrElse(axis, TreeMap.empty)

      val teamsSeenThisTurn = teamUpdates.map(update => Team(update.view.snapshots.map(_.id))).toSet

      val newTeamsSeenAxis = teamsSeenAxis + ((updateTime, teamsSeenThisTurn))
      val newCharsSeenAxis = charsSeenAxis + ((updateTime, charUpdates.map(_.id)))

      val newTeamsSeenState = teamsSeen.updated(axis, newTeamsSeenAxis)
      val newCharsSeenState = charsSeen.updated(axis, newCharsSeenAxis)

      context.become(process(newLaddersState, newCharsState, newTeamsSeenState, newCharsSeenState, subs))

      for (sub <- subs)
        sub ! msg

    case QueryState(axis: Axis, interval: Interval) =>
      val response = for {
        ladderAxis <- ladders.get(axis)
        charsAxis <- chars.get(axis)
        teamsSeenAxis <- teamsSeen.get(axis)
        charsSeenAxis <- charsSeen.get(axis)
      } yield {
          val teamIds = teamsSeenAxis.from(interval.start).until(interval.end + 1.second).values.flatten.toSet
          val teams = teamIds.map(ladderAxis).toSeq
          val charIds = charsSeenAxis.from(interval.start).until(interval.end + 1.second).values.flatten.toSet
          val chars = charIds.map(charsAxis).toSeq
          QueryStateResponse(axis, teams, chars)
        }

      sender ! response.getOrElse(QueryStateResponse(axis, Seq.empty, Seq.empty))

    case Init(laddersState, charsState, teamsSeenState, charsSeenState) =>
      context.become(process(laddersState, charsState, teamsSeenState, charsSeenState, subs))

    case InitFrom =>
      sender ! Init(ladders, chars, teamsSeen, charsSeen)

    case Subscribe =>
      context.become(process(ladders, chars, teamsSeen, charsSeen, subs + sender))

    case Unsubscribe =>
      context.become(process(ladders, chars, teamsSeen, charsSeen, subs - sender))

    case Announce =>
      sender ! InitFrom
      sender ! Subscribe
  }

}
