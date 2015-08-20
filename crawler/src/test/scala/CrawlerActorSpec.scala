import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.github.nscala_time.time.Imports._
import info.fotm.aether.{StorageAxis, AetherConfig, Storage}
import info.fotm.api.models.{Leaderboard, LeaderboardRow, Threes, US}
import info.fotm.crawler.CrawlerActor
import info.fotm.crawler.CrawlerActor._
import info.fotm.domain.{Axis, CharacterId}
import info.fotm.util.MemoryPersisted
import org.joda.time.{DateTime, Interval}
import org.scalatest._

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Success

class CrawlerActorSpec extends FlatSpec with Matchers {
  implicit lazy val system = ActorSystem(AetherConfig.crawlerSystemPath.name, AetherConfig.crawlerConfig)
  implicit lazy val timeout: Timeout = new Timeout(Duration(10, scala.concurrent.duration.SECONDS))

  def createFetch[A](seq: Seq[A]): (() => Future[A]) = {
    val current = seq.iterator
    () =>
      if (current.hasNext) Future.successful(current.next())
      else Future.failed(new NoSuchElementException)
  }

  def interval = new Interval(DateTime.now - 10.minutes, DateTime.now)

  val axis = Axis(US, Threes)

  def genPlayer(rating: Int = 1500, factionId: Int = 1): LeaderboardRow = {
    LeaderboardRow(
      ranking = 1,
      rating = rating,
      name = java.util.UUID.randomUUID().toString,
      realmId = 1,
      realmName = "123",
      realmSlug = "123",
      raceId = 1,
      classId = 1,
      specId = 1,
      factionId = factionId,
      genderId = 1,
      seasonWins = 0,
      seasonLosses = 0,
      weeklyWins = 0,
      weeklyLosses = 0
    )
  }

  val alliance = 1
  val horde = 0

  trait CrawlerTest {
    val horde1: List[LeaderboardRow] = (0 until 3).map(_ => genPlayer(1500, horde)).toList
    val horde2: List[LeaderboardRow] = (0 until 3).map(_ => genPlayer(1500, horde)).toList
    val alliance1: List[LeaderboardRow] = (0 until 3).map(_ => genPlayer(1500, alliance)).toList
    val alliance2: List[LeaderboardRow] = (0 until 3).map(_ => genPlayer(1500, alliance)).toList
    val allChars = horde1 ++ horde2 ++ alliance1 ++ alliance2

    def isAlliance(id: CharacterId): Boolean =
      (alliance1 ++ alliance2).find(_.name == id.name).fold(false) { c => true }

    def isHorde(id: CharacterId): Boolean =
      (horde1 ++ horde2).find(_.name == id.name).fold(false) { c => true }

    /*
    Test flow taking into account SeenEnhancer
    1. initial leaderboard
    2. horde team 1 and alliance team 1 -> teams: 0; chars: ht1, at1
    3. horde team 1 & 2 and alliance team 2 -> teams: ht1; chars: ht2, at1, at2
    4. horde team 1 & 2 and alliance team 1 -> teams: ht1, ht2, at1; chars: at2
     */

    val leaderboard1 = Leaderboard(allChars)

    val leaderboard2 = Leaderboard(
      horde1    .map(_.copy(rating = 1520, seasonWins = 1, weeklyWins = 1)) ++
      alliance1 .map(_.copy(rating = 1520, seasonWins = 1, weeklyWins = 1)) ++
      horde2 ++
      alliance2
    )

    val leaderboard3 = Leaderboard(
      horde1    .map(_.copy(rating = 1530, seasonWins = 2, weeklyWins = 2)) ++
      alliance1 .map(_.copy(rating = 1520, seasonWins = 1, weeklyWins = 1)) ++
      horde2    .map(_.copy(rating = 1520, seasonWins = 1, weeklyWins = 1)) ++
      alliance2 .map(_.copy(rating = 1520, seasonWins = 1, weeklyWins = 1))
    )

    val leaderboard4 = Leaderboard(
      horde1    .map(_.copy(rating = 1540, seasonWins = 3, weeklyWins = 3)) ++
      alliance1 .map(_.copy(rating = 1530, seasonWins = 2, weeklyWins = 2)) ++
      horde2    .map(_.copy(rating = 1530, seasonWins = 2, weeklyWins = 2)) ++
      alliance2 .map(_.copy(rating = 1520, seasonWins = 1, weeklyWins = 1))
    )

    val fetch = createFetch(Seq(leaderboard1, leaderboard2, leaderboard3, leaderboard4))

    val storageActor = TestActorRef[Storage](Props(classOf[Storage], new MemoryPersisted[Map[Axis, StorageAxis]]))
    val crawlerActor = TestActorRef[CrawlerActor](Props(classOf[CrawlerActor], storageActor, fetch, axis))

    def sleep() = Thread.sleep(500)
  }

  "crawler" should "return nothing after initial crawl" in new CrawlerTest {
    crawlerActor ! Crawl
    sleep()

    val queryFuture = storageActor ? Storage.QueryPlayingNow(axis, interval)
    val Success(response: Storage.QueryPlayingNowResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.teams.size should be(0)
    response.chars.size should be(0)
  }

  "crawler" should "return ht1 & at1 chars and no teams after second crawls" in new CrawlerTest {
    crawlerActor ! Crawl
    sleep()
    crawlerActor ! Crawl
    sleep()

    val queryFuture = storageActor ? Storage.QueryPlayingNow(axis, interval)
    val Success(response: Storage.QueryPlayingNowResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.teams.size should be(0)
    response.chars.map(_.id.name) should contain theSameElementsAs (horde1 ++ alliance1).map(_.name)
    response.chars.foreach(cs => cs.stats.rating should be(1520))
  }

  //     3. horde team 1 & 2 and alliance team 2 -> teams: ht1; chars: ht2, at1, at2
  "crawler" should "return h1 team and ht2, at1, at2 chars after third crawls" in new CrawlerTest {
    crawlerActor ! Crawl
    sleep()
    crawlerActor ! Crawl
    sleep()
    crawlerActor ! Crawl
    sleep()

    val queryFuture = storageActor ? Storage.QueryPlayingNow(axis, interval)
    val Success(response: Storage.QueryPlayingNowResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.teams.size should be(1)
    response.teams.foreach(_.team.members.size should be(3))
    response.teams.map(_.team.members.map(_.name).toSeq.sorted) should contain theSameElementsAs Seq(horde1.map(_.name).sorted)
    response.chars.map(_.id.name) should contain theSameElementsAs (horde2 ++ alliance1 ++ alliance2).map(_.name)
  }

  //    4. horde team 1 & 2 and alliance team 1 -> teams: ht1, ht2, at1; chars: at2
  "crawler" should "return ht1, ht2, at1 teams and at2 chars after fourth crawls" in new CrawlerTest {
    crawlerActor ! Crawl
    sleep()
    crawlerActor ! Crawl
    sleep()
    crawlerActor ! Crawl
    sleep()
    crawlerActor ! Crawl
    sleep()

    val queryFuture = storageActor ? Storage.QueryPlayingNow(axis, interval)
    val Success(response: Storage.QueryPlayingNowResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.teams.size should be(3)
    response.teams.foreach(_.team.members.size should be(3))
    response.teams.map(_.team.members.map(_.name).toSeq.sorted) should contain theSameElementsAs Seq(horde1, horde2, alliance1).map(_.map(_.name).sorted)
    response.chars.map(_.id.name) should contain theSameElementsAs alliance2.map(_.name)
  }

  // TODO: test recrawl
}
