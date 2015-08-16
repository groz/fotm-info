import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.github.nscala_time.time.Imports._
import info.fotm.aether.Storage
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
  implicit lazy val system = ActorSystem()
  implicit lazy val timeout: Timeout = new Timeout(Duration(1, scala.concurrent.duration.SECONDS))

  def createStorageActor = TestActorRef(Props(new Storage(new MemoryPersisted[Storage.PersistedStorageState])))

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
    val hordeChars = (0 until 3).map(_ => genPlayer(1500, horde)).toList
    val allianceChars: List[LeaderboardRow] = (0 until 3).map(_ => genPlayer(1500, alliance)).toList
    val allChars = hordeChars ++ allianceChars

    def isAlliance(id: CharacterId): Boolean = allianceChars.find(_.name == id.name).fold(false) { c =>
      true
    }

    def isHorde(id: CharacterId): Boolean = hordeChars.find(_.name == id.name).fold(false) { c =>
      true
    }

    val startingLeaderboard = Leaderboard(allChars)

    val leaderboard1 = Leaderboard(allChars.map(_.copy(rating = 1520, seasonWins = 1, weeklyWins = 1)))

    val leaderboard2 = Leaderboard(
      hordeChars.map(_.copy(rating = 1530, seasonWins = 2, weeklyWins = 2)) ++
      allianceChars.map(_.copy(rating = 1520, seasonWins = 1, weeklyWins = 1))
    )

    val leaderboard3 = Leaderboard(allChars.map(_.copy(rating = 1530, seasonWins = 2, weeklyWins = 2)))

    val fetch = createFetch(Seq(startingLeaderboard, leaderboard1, leaderboard2, leaderboard3))

    val storageActor = createStorageActor
    val crawlerActor = TestActorRef(Props(classOf[CrawlerActor], storageActor, fetch, axis))
  }

  "crawler" should "return no chars and no teams after initial crawl" in new CrawlerTest {
    crawlerActor ! Crawl

    val queryFuture = storageActor ? Storage.QueryState(axis, interval)
    val Success(response: Storage.QueryStateResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.teams.size should be(0)
    response.chars.size should be(0)
  }

  "crawler" should "return all chars and no teams after 2 crawls" in new CrawlerTest {
    crawlerActor ! Crawl
    Thread.sleep(100)
    crawlerActor ! Crawl
    Thread.sleep(100)

    val queryFuture = storageActor ? Storage.QueryState(axis, interval)
    val Success(response: Storage.QueryStateResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.teams.size should be(0)
    response.chars.size should be(allChars.size)
    response.chars.foreach(cs => cs.stats.rating should be(1520))
  }

  "crawler" should "return one horde team and 3 alliance chars after 3 crawls" in new CrawlerTest {
    crawlerActor ! Crawl
    Thread.sleep(100)
    crawlerActor ! Crawl
    Thread.sleep(100)
    crawlerActor ! Crawl
    Thread.sleep(100)

    val queryFuture = storageActor ? Storage.QueryState(axis, interval)
    val Success(response: Storage.QueryStateResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.teams.size should be(1)
    response.teams.foreach(_.rating should be(1530))
    response.teams.foreach(_.team.members.size should be(3))
    response.teams.foreach(_.team.members.forall(isHorde))
    response.chars.size should be(3)
    response.chars.forall(cs => isAlliance(cs.id)) should be(true)
  }

  "crawler" should "return two teams and 0 chars after 4 crawls" in new CrawlerTest {
    crawlerActor ! Crawl
    Thread.sleep(100)
    crawlerActor ! Crawl
    Thread.sleep(100)
    crawlerActor ! Crawl
    Thread.sleep(100)
    crawlerActor ! Crawl
    Thread.sleep(100)

    val queryFuture = storageActor ? Storage.QueryState(axis, interval)
    val Success(response: Storage.QueryStateResponse) = queryFuture.value.get

    response.axis should be(axis)
    response.teams.size should be(2)
    response.teams.foreach(_.rating should be(1530))
    response.teams.foreach(_.team.members.size should be(3))
    response.teams.foreach(t => t.team.members.forall(isHorde) || t.team.members.forall(isAlliance))
    response.chars.size should be(0)
  }

  // TODO: test recrawl
}
