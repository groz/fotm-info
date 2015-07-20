package info.fotm.aether

import akka.actor.{ActorRef, Actor}
import akka.event.{Logging, LoggingReceive}
import info.fotm.domain._

import scala.collection.breakOut

object Storage {
  // tables
  type TeamLadderAxis = Map[Team, TeamSnapshot]
  type CharSnapshotsAxis = Map[CharacterId, CharacterSnapshot]

  // init flows
  final case class Init(ladders: Map[Axis, TeamLadderAxis], chars: Map[Axis, CharSnapshotsAxis])

  case object InitFrom

  // input
  final case class Updates(axis: Axis, teamUpdates: Seq[TeamUpdate], charUpdates: Set[CharacterDiff])

  // output
  final case class QueryState(axis: Axis)

  final case class QueryStateResponse(axis: Axis, teamLadder: TeamLadderAxis, chars: CharSnapshotsAxis)

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

  override def receive: Receive = process(Map.empty, Map.empty, Set.empty)

  def process(ladders: Map[Axis, Map[Team, TeamSnapshot]],
              chars: Map[Axis, Map[CharacterId, CharacterSnapshot]],
              subs: Set[ActorRef])
  : Receive = LoggingReceive {
    case Updates(axis, teamUpdates, charUpdates) =>
      log.debug("Updates received. Processing...")
      val ladderAxis: Map[Team, TeamSnapshot] = ladders.getOrElse(axis, Map.empty)

      val updatedSnapshots = for {
        update <- teamUpdates
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

      val newLadderAxis = ladderAxis ++ updatedSnapshots.map(ts => (ts.team, ts))

      val charsAxis: CharSnapshotsAxis = chars.getOrElse(axis, Map.empty)
      val newCharsAxis = charsAxis ++ charUpdates.map(cu => (cu.id, cu.current))(breakOut)

      val newLaddersState = ladders.updated(axis, newLadderAxis)
      val newCharsState = chars.updated(axis, newCharsAxis)
      context.become(process(newLaddersState, newCharsState, subs))

      for (sub <- subs)
        sub ! Updates(axis, teamUpdates, charUpdates)

    case QueryState(axis: Axis) =>
      ladders.get(axis).foreach { ladder =>
        sender ! QueryStateResponse(axis, ladder, chars.getOrElse(axis, Map.empty))
      }

    case Init(laddersState, charsState) =>
      context.become(process(laddersState, charsState, subs))

    case InitFrom =>
      sender ! Init(ladders, chars)

    case Subscribe =>
      context.become(process(ladders, chars, subs + sender))

    case Unsubscribe =>
      context.become(process(ladders, chars, subs - sender))

    case Announce =>
      sender ! InitFrom
      sender ! Subscribe
  }

}
