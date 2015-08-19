import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.github.nscala_time.time.Imports._
import info.fotm.aether.{Storage, StorageAxis}
import info.fotm.clustering.ClusteringEvaluatorData
import info.fotm.domain._
import info.fotm.util.MemoryPersisted
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration.Duration
import scala.util.Success

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

  def createStorageActor = TestActorRef(Props(new Storage(new MemoryPersisted[Map[Axis, StorageAxis]])))

  "Updates" should "correctly populate Storage" in {
    // 1
    val storageActorRef = createStorageActor

    storageActorRef ! Storage.Updates(axis, teamUpdates, charUpdates)

    val now = DateTime.now

    val queryFuture = storageActorRef ? Storage.QueryState(axis, new Interval(now - 1.month, now))
    val Success(response: Storage.QueryStateResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.teams should contain theSameElementsAs Seq(currentTeam)
    response.chars.size should be(0)
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
    response.teams should contain theSameElementsAs Seq(nextTeam)
    response.chars.size should be(0)
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
    response.teams should contain theSameElementsAs Seq(currentTeam)
    response.chars.size should be(0)
  }

  "Announce" should "subscribe proxy to updates" in {
    val storageActorRef = createStorageActor
    val storageProxyActorRef = createStorageActor

    storageProxyActorRef.tell(Storage.Announce, storageActorRef)
    storageActorRef ! Storage.Updates(axis, teamUpdates, charUpdates)

    val now = DateTime.now

    // query proxy
    val queryFuture = storageProxyActorRef ? Storage.QueryState(axis, new Interval(now - 1.month, now))
    val Success(response: Storage.QueryStateResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.teams should contain theSameElementsAs Seq(currentTeam)
    response.chars.size should be(0)
  }

}
