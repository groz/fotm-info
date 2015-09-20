import info.fotm.aether.StorageAxis
import com.github.nscala_time.time.Imports._
import info.fotm.clustering.ClusteringEvaluatorData
import info.fotm.domain._
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable.TreeMap

class StorageAxisSpec extends FlatSpec with Matchers {

  trait StorageAxisTest {
    val emptyStorageAxis: StorageAxis = StorageAxis()

    val createChar: Int => CharacterSnapshot = {
      val data = new ClusteringEvaluatorData()
      rating => data.genPlayer(rating)
    }

    val now = DateTime.now
    val updateTime1 = now - 20.seconds
    val updateTime2 = now - 1.second

    val queryInterval0 = new Interval(now - 50.seconds, now - 40.seconds)
    val queryIntervalFirst = new Interval(now - 30.seconds, now - 10.seconds)
    val queryIntervalBoth = new Interval(now - 30.seconds, now)

    val nChars = 4
    val teamSize = 2

    val charSnapshots1: Set[CharacterSnapshot] = (0 until nChars).map(_ => createChar(1500)).toSet
    val teamViews1: Set[TeamView] = charSnapshots1.grouped(2).map(cs => TeamView(cs)).toSet
    val teamUpdates1 = teamViews1.map(TeamUpdate(_, true))
    val storageAxis1 = emptyStorageAxis.update(teamUpdates1.toSeq, charSnapshots1, updateTime1)

    val charSnapshots2 = charSnapshots1.map(c => c.copy(stats = c.stats.update(20)))
    val teamViews2: Set[TeamView] = charSnapshots2.grouped(2).map(cs => TeamView(cs)).toSet
    val teamUpdates2 = teamViews2.map(TeamUpdate(_, true))
    val storageAxis2 = storageAxis1.update(teamUpdates2.toSeq, charSnapshots2, updateTime2)
  }

  "inInterval" should "correctly return items in first interval" in new StorageAxisTest {
    val tree = TreeMap(updateTime1 -> 1, updateTime2 -> 2)
    val expected = Seq(1)
    val actual = StorageAxis.inInterval(tree, queryIntervalFirst)
    actual.toSeq should be(expected)
  }

  it should "correctly return items in global interval" in new StorageAxisTest {
    val tree = TreeMap(updateTime1 -> 1, updateTime2 -> 2)
    val expected = Seq(1, 2)
    val actual = StorageAxis.inInterval(tree, queryIntervalBoth)
    actual.toSeq should be(expected)
  }

  it should "correctly return no items for 0 interval" in new StorageAxisTest {
    val tree = TreeMap(updateTime1 -> 1, updateTime2 -> 2)
    val expected = Seq.empty
    val actual = StorageAxis.inInterval(tree, queryInterval0)
    actual.toSeq should be(expected)
  }

  "Updates" should "correctly populate Storage" in new StorageAxisTest {
    // step 1
    val expectedCharsSeen1 = TreeMap(updateTime1 -> charSnapshots1.map(_.id))
    storageAxis1.charsSeen should be(expectedCharsSeen1)

    val expectedCharHistories1 = charSnapshots1.map { cs => (cs.id, TreeMap(updateTime1 -> cs)) }.toMap
    storageAxis1.charHistories should be(expectedCharHistories1)

    val expectedTeamsSeen1 = TreeMap(updateTime1 -> teamViews1.map(_.teamId))
    storageAxis1.teamsSeen should be(expectedTeamsSeen1)

    val expectedTeamHistories1 =
      teamUpdates1.map { tu => (tu.view.teamId, TreeMap(updateTime1 -> TeamSnapshot.fromUpdate(tu))) }.toMap
    storageAxis1.teamHistories should be(expectedTeamHistories1)

    // step 2
    val expectedCharsSeen2 = TreeMap(updateTime1 -> charSnapshots1.map(_.id), updateTime2 -> charSnapshots2.map(_.id))
    storageAxis2.charsSeen should be(expectedCharsSeen2)

    val expectedCharHistories2 = charSnapshots2.map { cs =>
      (cs.id, expectedCharHistories1(cs.id) ++ TreeMap(updateTime2 -> cs))
    }.toMap
    storageAxis2.charHistories should be(expectedCharHistories2)

    val expectedTeamsSeen2 = TreeMap(updateTime1 -> teamViews1.map(_.teamId), updateTime2 -> teamViews2.map(_.teamId))
    storageAxis2.teamsSeen should be(expectedTeamsSeen2)

    val expectedTeamHistories2 = teamUpdates2.map { tu =>
      (tu.view.teamId, expectedTeamHistories1(tu.view.teamId) ++ TreeMap(updateTime2 -> {
        val ts = TeamSnapshot.fromUpdate(tu)
        ts.copy(stats = ts.stats.win)
      }))
    }.toMap
    storageAxis2.teamHistories should be(expectedTeamHistories2)
  }

  "teams" should "return latest snapshots for global interval" in new StorageAxisTest {
    val expectedTeams: Set[TeamSnapshot] = teamUpdates2.map(tu => TeamSnapshot.fromUpdate(tu).copy(stats = Stats(2, 0)))
    val actualTeams: Set[TeamSnapshot] = storageAxis2.teams(queryIntervalBoth)
    actualTeams should be(expectedTeams)
  }

  it should "be empty for interval outside changes" in new StorageAxisTest {
    val actual = storageAxis2.teams(queryInterval0)
    actual.size should be(0)
  }

  it should "return init snapshots for first interval" in new StorageAxisTest {
    val expectedTeams: Set[TeamSnapshot] = teamUpdates1.map(tu => TeamSnapshot.fromUpdate(tu))
    val actualTeams: Set[TeamSnapshot] = storageAxis2.teams(queryIntervalFirst)
    actualTeams should be(expectedTeams)
  }

  "setups" should "return 1.0 ratio for default setup" in new StorageAxisTest {
    val expected = Set( FotmSetup(Seq(1, 1), 1.0) )
    val actual: Seq[FotmSetup] = storageAxis2.setups(queryIntervalBoth)
    actual.toSet should be(expected)
  }

  it should "be empty for interval outside changes" in new StorageAxisTest {
    val actual = storageAxis2.setups(queryInterval0)
    actual.size should be(0)
  }

  "teamHistory" should "be empty for interval outside changes" in new StorageAxisTest {
    val actual = storageAxis2.teamHistory(queryInterval0, teamViews1.head.teamId)
    actual.size should be(0)
  }

  it should "be empty for team not seen" in new StorageAxisTest {
    val actual = storageAxis2.teamHistory(queryIntervalBoth, Team(Set.empty))
    actual.size should be(0)
  }

  it should "return first entry for first interval" in new StorageAxisTest {
    val teamId = teamViews1.head.teamId
    val expected = Seq(TeamSnapshot.fromUpdate(teamUpdates1.head))
    val actual = storageAxis2.teamHistory(queryIntervalFirst, teamId)
    actual should be(expected)
  }

  it should "return both entries for global interval" in new StorageAxisTest {
    val teamId = teamViews1.head.teamId
    val ts1 = TeamSnapshot.fromUpdate(teamUpdates1.head)
    val ts2 = TeamSnapshot.fromUpdate(teamUpdates2.head).copy(stats = Stats(2, 0))
    val expected = Set(ts1, ts2)
    val actual = storageAxis2.teamHistory(queryIntervalBoth, teamId).toSet
    actual should be(expected)
  }

  "charHistory" should "be empty for interval outside changes" in new StorageAxisTest {
    val actual = storageAxis2.charHistory(queryInterval0, charSnapshots1.head.id)
    actual.size should be(0)
  }

  it should "be empty for char not seen" in new StorageAxisTest {
    val actual = storageAxis2.charHistory(queryInterval0, charSnapshots1.head.id.copy(name = "unknown name"))
    actual.size should be(0)
  }

  it should "return first entry for first interval" in new StorageAxisTest {
    val charId = charSnapshots1.head.id
    val expected = Seq(charSnapshots1.head)
    val actual: Seq[CharacterSnapshot] = storageAxis2.charHistory(queryIntervalFirst, charId)
    actual should be(expected)
  }

  it should "return both entries for global interval" in new StorageAxisTest {
    val charId = charSnapshots1.head.id
    val expected = Seq(charSnapshots1.head, charSnapshots2.head)
    val actual: Seq[CharacterSnapshot] = storageAxis2.charHistory(queryIntervalBoth, charId)
    actual should be(expected)
  }

  "chars" should "be empty for interval outside changes" in new StorageAxisTest {
    val actual = storageAxis2.chars(queryInterval0)
    actual.size should be(0)
  }

  it should "return latest snapshots for global interval" in new StorageAxisTest {
    val expected: Set[CharacterSnapshot] = charSnapshots2
    val actual: Seq[CharacterSnapshot] = storageAxis2.chars(queryIntervalBoth)
    actual.toSet should be(expected)
  }

  it should "return init snapshots for first interval" in new StorageAxisTest {
    val expected: Set[CharacterSnapshot] = charSnapshots1
    val actual: Seq[CharacterSnapshot] = storageAxis2.chars(queryIntervalFirst)
    actual.toSet should be(expected)
  }
}
