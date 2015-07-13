package info.fotm.aether

import akka.actor.{ActorRef, Actor}
import info.fotm.domain.{Team, TeamLadder, Axis, TeamSnapshot}

object Storage {
  // input
  final case class NewTeams(axis: Axis, teamSnapshots: Seq[TeamSnapshot])

  // output
  final case class GetTeamLadder(axis: Axis)
  final case class TeamLadderResponse(teamLadder: TeamLadder)

  // reactive, subscribes/unsubscribes sender to updates
  case object Subscribe
  case object Unsubscribe
}

class Storage extends Actor {
  import Storage._

  // TODO: save/load state on init

  override def receive: Receive = process(Map.empty, Set.empty)

  def process(ladders: Map[Axis, Map[Team, TeamSnapshot]], subs: Set[ActorRef]): Receive = {

    case NewTeams(axis, teamSnapshots: Seq[TeamSnapshot]) =>
      val axisLadder: Map[Team, TeamSnapshot] = ladders.getOrElse(axis, Map.empty)
      val newAxisLadder = axisLadder ++ teamSnapshots.map(ts => (ts.team, ts))
      context.become( process(ladders.updated(axis, newAxisLadder), subs) )

      for (sub <- subs)
        sub ! NewTeams(axis, teamSnapshots)

    case GetTeamLadder(axis: Axis) =>
      ladders.get(axis).fold {
        sender ! TeamLadderResponse(TeamLadder(axis, Map.empty))
      } { ladder =>
        sender ! TeamLadderResponse(TeamLadder(axis, ladder))
      }

    case Subscribe =>
      context.become( process(ladders, subs + sender) )

    case Unsubscribe =>
      context.become( process(ladders, subs - sender) )
  }

}
