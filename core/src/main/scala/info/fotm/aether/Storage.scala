package info.fotm.aether

import akka.actor.{Actor, ActorIdentity, ActorRef, Props}
import akka.event.{Logging, LoggingReceive}
import com.github.nscala_time.time.Imports._
import com.twitter.bijection.{Bijection, GZippedBytes}
import info.fotm.domain._
import info.fotm.util._
import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs.implicits._

import scala.collection.immutable.TreeMap

object Storage {
  val props: Props = Props[Storage]
  val readonlyProps: Props = Props(classOf[Storage], true)

  val identifyMsgId = "storage"
  val Identify = akka.actor.Identify(identifyMsgId)

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

  val keyPathBijection =
    Bijection.build[Axis, String] { axis => s"${axis.region.slug}/${axis.bracket.slug}" } { str =>
      val Array(r, b) = str.split('/')
      Axis.parse(r, b).get
    }

  import info.fotm.MyCodecImplicits._

  def scodecGzipBijection[T](implicit codec: Codec[T]): Bijection[T, Array[Byte]] = Bijection.build[T, Array[Byte]] { t =>
    val bytes = Codec.encode(t).require.toByteArray
    Bijection.bytes2GzippedBytes(bytes).bytes
  } { gzippedBytes =>
    val bytes = Bijection.bytes2GzippedBytes.inverse(GZippedBytes(gzippedBytes))
    Codec.decode[T](BitVector(bytes)).require.value
  }

  lazy val fromConfig =
    AetherConfig.storagePersistence[Axis, StorageAxisState](keyPathBijection, scodecGzipBijection[StorageAxisState])
}

case class StorageAxisState(teams: Map[Team, TeamSnapshot] = Map.empty,
                            chars: Map[CharacterId, CharacterSnapshot] = Map.empty,
                            teamsSeen: TreeMap[DateTime, Set[Team]] = TreeMap.empty,
                            charsSeen: TreeMap[DateTime, Set[CharacterId]] = TreeMap.empty)

class Storage(persistence: Persisted[Map[Axis, StorageAxisState]]) extends Actor {

  import Storage._

  def this(proxy: Boolean) = this(if (proxy) Storage.fromConfig.readonly else Storage.fromConfig)

  def this() = this(false)

  val log = Logging(context.system, this.getClass)

  override def receive: Receive = {
    val init = Axis.all.map { (_, StorageAxisState()) }.toMap

    val state = init ++ persistence.fetch().fold(Map.empty[Axis, StorageAxisState])(identity)

    process(state, Set.empty)
  }

  def process(state: Map[Axis, StorageAxisState], subs: Set[ActorRef]): Receive = LoggingReceive {

    case msg@Updates(axis, teamUpdates: Seq[TeamUpdate], charUpdates) =>
      log.debug("Updates received. Processing...")
      val updateTime: DateTime = DateTime.now

      val currentAxis = state(axis)

      val updatedTeamSnapshots: Seq[TeamSnapshot] =
        for {
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

      persistence.save(Map(axis -> updatedState)) // save only changed axis

      context.become(process(state.updated(axis, updatedState), subs))

      for (sub <- subs)
        sub ! msg

    case QueryState(axis: Axis, interval: Interval) =>
      val currentAxis: StorageAxisState = state(axis)

      val teamIds: Set[Team] =
        currentAxis
          .teamsSeen
          .from(interval.start).until(interval.end + 1.second)
          .values.flatten.toSet

      val teams: Set[TeamSnapshot] = teamIds.map(currentAxis.teams) //.filter(_.stats.total > 1)

      val charIds: Set[CharacterId] =
        currentAxis
          .charsSeen
          .from(interval.start).until(interval.end + 1.second)
          .values.flatten.toSet

      // filter out chars seen in teams that are sent back
      val charsInTeams: Set[CharacterId] = teams.flatMap(_.team.members)
      val chars: Set[CharacterSnapshot] = (charIds diff charsInTeams).map(currentAxis.chars)

      sender ! QueryStateResponse(axis, teams.toSeq.sortBy(-_.rating), chars.toSeq.sortBy(-_.stats.rating))

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
