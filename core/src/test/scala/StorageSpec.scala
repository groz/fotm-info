import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import info.fotm.aether.Storage
import info.fotm.clustering.ClusteringEvaluatorData
import info.fotm.domain._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration.Duration
import scala.util.Success

import com.github.nscala_time.time.Imports._

/*
Testing workflows:

√ 1) Regular workflow:
    Updates should populate storage
    QueryState should trigger QueryStateResponse with whatever was sent in Updates

    Messages:
    final case class Updates(axis: Axis, teamUpdates: Seq[TeamUpdate], charUpdates: Set[CharacterDiff])
    final case class QueryState(axis: Axis, interval: Interval)
    final case class QueryStateResponse(axis: Axis, teams: Seq[TeamSnapshot], chars: Seq[CharacterSnapshot])

√ 1a) Regular workflow works with time intervals

√ 1b) Regular workflow should show last one of consecutive updates

√ 1c) Should respond with empty if the axis was not found

√ 2) Init workflow:
    When sent Identify, it should reply with Init(state) and Subscribe to udpates

    Messages:
    final case class Init(ladders: Map[Axis, TeamLadderAxis], chars: Map[Axis, CharSnapshotsAxis])
    val Identify

√ 3) Subscribe workflow
    Subscriber should receive Updates when the original gets them

    Messages:
    case object Subscribe
    case object Unsubscribe

√ 4) Announcement
    When Storage receives Announce message it should send InitFrom and Subscribe requests to sender
    case object Announce

 */

class StorageSpec extends FlatSpec with Matchers {

  implicit lazy val system = ActorSystem()
  implicit lazy val timeout: Timeout = new Timeout(Duration(1, scala.concurrent.duration.SECONDS))

  val previousSnapshot: CharacterSnapshot = new ClusteringEvaluatorData().genPlayer(1500)
  val currentSnapshot: CharacterSnapshot = previousSnapshot

  val currentTeam = TeamSnapshot.fromSnapshots(Set(currentSnapshot)).copy(stats = Stats.empty.win)
  val nextTeam = currentTeam.copy(stats = Stats.empty.win.loss)

  val teamUpdates: Seq[TeamUpdate] = Seq(TeamUpdate(currentTeam.view, won = true))
  val nextTeamUpdates: Seq[TeamUpdate] = Seq(TeamUpdate(nextTeam.view, won = false))
  val charUpdates: Set[CharacterDiff] = Set(CharacterDiff(previousSnapshot, currentSnapshot))

  val axis = Axis.all.head

  def createStorageActor = TestActorRef(Props(new Storage()))

  "Updates" should "correctly populate Storage" in {
    // 1
    val storageActorRef = createStorageActor

    storageActorRef ! Storage.Updates(axis, teamUpdates, charUpdates)

    val now = DateTime.now

    val queryFuture = storageActorRef ? Storage.QueryState(axis, new Interval(now - 1.month, now))
    val Success(response: Storage.QueryStateResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.chars should contain theSameElementsAs Seq(currentSnapshot)
    response.teams should contain theSameElementsAs Seq(currentTeam)
  }

  it should "correctly handle time intervals" in {
    // 1a
    val storageActorRef = createStorageActor

    val now = DateTime.now // there were no updates before this time

    storageActorRef ! Storage.Updates(axis, teamUpdates, charUpdates)

    val queryFuture = storageActorRef ? Storage.QueryState(axis, new Interval(now - 1.month, now - 1.second))
    val Success(response: Storage.QueryStateResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.chars.size should be(0)
    response.teams.size should be(0)
  }

  it should "show last of consecutive updates" in {
    // 1b
    val storageActorRef = createStorageActor

    storageActorRef ! Storage.Updates(axis, teamUpdates, charUpdates)
    storageActorRef ! Storage.Updates(axis, nextTeamUpdates, charUpdates)

    val now = DateTime.now

    val queryFuture = storageActorRef ? Storage.QueryState(axis, new Interval(now - 1.month, now))
    val Success(response: Storage.QueryStateResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.chars should contain theSameElementsAs Seq(currentSnapshot)
    response.teams should contain theSameElementsAs Seq(nextTeam)
  }

  it should "respond with empty if Axis is not found or axis is empty" in {
    // 1b
    val storageActorRef = createStorageActor

    val now = DateTime.now

    val queryFuture = storageActorRef ? Storage.QueryState(axis, new Interval(now - 1.month, now))
    val Success(response: Storage.QueryStateResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.chars.size should be(0)
    response.teams.size should be(0)
  }

  "Identify" should "send all data back to the proxy" in {
    // 2
    val storageActorRef = createStorageActor
    val storageProxyActorRef = createStorageActor

    // populate storage
    storageActorRef ! Storage.Updates(axis, teamUpdates, charUpdates)

    // lookup storage from proxy
    storageActorRef.tell(Storage.Identify, storageProxyActorRef)

    // check that proxy received all the data that is in storage
    val now = DateTime.now
    val queryFuture = storageProxyActorRef ? Storage.QueryState(axis, new Interval(now - 1.month, now))
    val Success(response: Storage.QueryStateResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.chars should contain theSameElementsAs Seq(currentSnapshot)
    response.teams should contain theSameElementsAs Seq(currentTeam)
  }

  "Subscribe" should "subscribe sender to updates" in {
    val storageActorRef = createStorageActor
    val storageProxyActorRef = createStorageActor

    storageActorRef.tell(Storage.Subscribe, storageProxyActorRef)
    storageActorRef ! Storage.Updates(axis, teamUpdates, charUpdates)

    val now = DateTime.now

    // query proxy
    val queryFuture = storageProxyActorRef ? Storage.QueryState(axis, new Interval(now - 1.month, now))
    val Success(response: Storage.QueryStateResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.chars should contain theSameElementsAs Seq(currentSnapshot)
    response.teams should contain theSameElementsAs Seq(currentTeam)
  }

  "Announce" should "populate proxy" in {
    val storageActorRef = createStorageActor
    storageActorRef ! Storage.Updates(axis, teamUpdates, charUpdates)

    val storageProxyActorRef = createStorageActor
    storageProxyActorRef.tell(Storage.Announce, storageActorRef)

    val now = DateTime.now

    // query proxy
    val queryFuture = storageProxyActorRef ? Storage.QueryState(axis, new Interval(now - 1.month, now))
    val Success(response: Storage.QueryStateResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.chars should contain theSameElementsAs Seq(currentSnapshot)
    response.teams should contain theSameElementsAs Seq(currentTeam)
  }

}
