package info.fotm.aether

import akka.actor.{Actor, ActorIdentity, ActorRef, Props}
import akka.event.{Logging, LoggingReceive}
import com.github.nscala_time.time.Imports
import com.github.nscala_time.time.Imports._
import com.twitter.bijection.{Base64String, Bijection, GZippedBytes}
import info.fotm.domain.TeamSnapshot.SetupFilter
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

  final case class QueryAll(axis: Axis, interval: Interval, filter: SetupFilter)
  final case class QueryAllResponse(axis: Axis, setups: Seq[FotmSetup], teams: Seq[TeamSnapshot], chars: Seq[CharacterSnapshot])

  final case class QueryTeamHistory(axis: Axis, team: Team)
  final case class QueryTeamHistoryResponse(axis: Axis, team: Team, history: TreeMap[DateTime, TeamSnapshot])

  final case class QueryCharHistory(axis: Axis, id: CharacterId)
  final case class QueryCharHistoryResponse(axis: Axis, charId: CharacterId, lastSnapshot: Option[CharacterSnapshot], history: TreeMap[DateTime, TeamSnapshot])

  final case class QueryFotm(axis: Axis, interval: Interval)
  final case class QueryFotmResponse(axis: Axis, setups: Seq[FotmSetup])

  // output
  final case class QueryPlayingNow(axis: Axis, interval: Interval)
  final case class QueryPlayingNowResponse(axis: Axis, teams: Seq[TeamSnapshot], chars: Seq[CharacterSnapshot])

  // reactive, subscribes/unsubscribes sender to updates
  case object Subscribe

  case object Unsubscribe

  // I'm online (again?)!
  case object Announce

  val keyPathBijection =
    Bijection.build[Axis, String] { axis =>
      s"${axis.region.slug}/${axis.bracket.slug}"
    } { str =>
      val Array(r, b) = str.split('/')
      Axis.parse(r, b).get
    }

  import info.fotm.MyCodecImplicits._

  def scodecBase64Bijection[T](implicit codec: Codec[T]): Bijection[T, String] = Bijection.build[T, String] { t =>
    val bytes = Codec.encode(t).require.toByteArray
    Bijection.bytes2Base64(bytes).str
  } { base64 =>
    val bytes = Bijection.bytes2Base64.inverse(Base64String(base64))
    Codec.decode[T](BitVector(bytes)).require.value
  }

  def scodecGzipBijection[T](implicit codec: Codec[T]): Bijection[T, Array[Byte]] = Bijection.build[T, Array[Byte]] { t =>
    val bytes = Codec.encode(t).require.toByteArray
    Bijection.bytes2GzippedBytes(bytes).bytes
  } { gzippedBytes =>
    val bytes = Bijection.bytes2GzippedBytes.inverse(GZippedBytes(gzippedBytes))
    Codec.decode[T](BitVector(bytes)).require.value
  }

  lazy val teamIdBijection: Bijection[Team, String] = scodecBase64Bijection[Team]
  lazy val charIdBijection: Bijection[CharacterId, String] = scodecBase64Bijection[CharacterId]

  lazy val fromConfig =
    AetherConfig.storagePersistence[Axis, StorageAxis](keyPathBijection, scodecGzipBijection[StorageAxis])
}

class Storage(persistence: Persisted[Map[Axis, StorageAxis]]) extends Actor {

  import Storage._

  def this(proxy: Boolean) = this(if (proxy) Storage.fromConfig.readonly else Storage.fromConfig)

  def this() = this(false)

  val log = Logging(context.system, this.getClass)

  override def receive: Receive = {
    val init = Axis.all.map { (_, StorageAxis()) }.toMap

    val state = init ++ persistence.fetch().recover {
      case ex =>
        log.debug(s"Initial load from storage failed with $ex")
        Map.empty[Axis, StorageAxis]
    }.get

    process(state, Set.empty)
  }

  def process(state: Map[Axis, StorageAxis], subs: Set[ActorRef]): Receive = LoggingReceive {

    case msg@Updates(axis, teamUpdates: Seq[TeamUpdate], charUpdates: Set[CharacterDiff]) =>
      log.debug("Updates received. Processing...")
      val storageAxis = state(axis)

      val updatedState = storageAxis.update(teamUpdates, charUpdates.map(_.current))
      // save only changed axis
      persistence.save(Map(axis -> updatedState)).recover {
        case ex => log.debug(s"Saving to storage failed with $ex")
      }

      context.become(process(state.updated(axis, updatedState), subs))

      for (sub <- subs)
        sub ! msg

    case QueryTeamHistory(axis: Axis, team: Team) =>
      val storageAxis = state(axis)
      sender ! QueryTeamHistoryResponse(axis, team, storageAxis.teamHistories(team))

    case QueryCharHistory(axis: Axis, charId: CharacterId) =>
      val storageAxis = state(axis)

      val histories: TreeMap[DateTime, TeamSnapshot] =
        storageAxis
          .teamHistories.filterKeys(_.members.contains(charId)).values
          .filter(_.size >= 2)
          .foldLeft(TreeMap.empty[DateTime, TeamSnapshot]) { _ ++ _ }

      val lastSnapshot = storageAxis.charHistories.get(charId).flatMap(_.lastOption.map(_._2))

      sender ! QueryCharHistoryResponse(axis, charId, lastSnapshot, histories)

    case QueryAll(axis: Axis, unadjustedInterval: Interval, setupFilter) =>
      val interval = new Interval(unadjustedInterval.start, unadjustedInterval.end + 100.millis)
      val storageAxis = state(axis)
      val (setups, teams, chars) = storageAxis.all(interval, cutoff = 2, setupFilter)
      sender ! QueryAllResponse(axis, setups, teams, chars)

    case QueryPlayingNow(axis: Axis, unadjustedInterval: Interval) =>
      val interval = new Interval(unadjustedInterval.start, unadjustedInterval.end + 100.millis)
      val storageAxis = state(axis)
      val teams: Set[TeamSnapshot] = storageAxis.teams(interval)
      val chars: Map[CharacterId, CharacterSnapshot] =
        storageAxis.chars(interval).map(c => (c.id, c))(scala.collection.breakOut)

      // filter out chars seen in teams that are sent back
      val charsInTeams: Set[CharacterId] = teams.flatMap(_.team.members)
      val charsNotInTeams = (chars -- charsInTeams).values.toSeq

      sender ! QueryPlayingNowResponse(axis, teams.toSeq.sortBy(-_.rating), charsNotInTeams.toSeq.sortBy(-_.stats.rating))

    case QueryFotm(axis: Axis, interval: Interval) =>
      val storageAxis = state(axis)
      val setups: Seq[FotmSetup] = storageAxis.setups(interval, cutoff = 2).sortBy(- _.ratio)
      sender ! QueryFotmResponse(axis, setups)

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
