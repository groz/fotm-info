import info.fotm.clustering._
import info.fotm.domain.LadderUpdate
import info.fotm.util.MathVector
import org.scalatest._

import scala.collection.immutable.Iterable

class ClusteringEvaluatorSpec extends FlatSpec with Matchers with ClusteringEvaluatorSpecBase {
  import gen._

  "evaluateStep" should "return correct metrics when everything is guessed right" in {
    val games = Set((team1580, team1500))

    val nextLadder = play(ladder, team1580, team1500)

    val evaluator = new ClusteringEvaluator(List(Feature.const[CharacterStatsUpdate]))

    val metrics = evaluator.evaluateStep(RealClusterer.identity, ladder, nextLadder, games)

    metrics.truePositive should be(2)
    metrics.falsePositive should be(0)
    metrics.falseNegative should be(0)
  }

  "splitIntoBuckets" should "correctly split ladder update into buckets" in {
    val current = play(ladder, team1580, team1500)
    val ladderUpdate = LadderUpdate(ladder, current)

    val expectedBuckets: Seq[Set[CharacterStatsUpdate]] = Seq(
      team1580.members.map(id => CharacterStatsUpdate(id, ladder(id), current(id))),
      team1500.members.map(id => CharacterStatsUpdate(id, ladder(id), current(id)))
    )

    val evaluator = new ClusteringEvaluator(Nil)
    val buckets: Seq[Set[CharacterStatsUpdate]] = evaluator.splitIntoBuckets(ladderUpdate).toSeq

    buckets should contain theSameElementsAs expectedBuckets
  }

  it should "correctly split ladder update with factions into buckets" in {
    val c1 = play(ladder, team1580, team1500)
    val current = play(c1, hTeam1580, hTeam1500)
    val ladderUpdate = LadderUpdate(ladder, current)

    val expectedBuckets: Seq[Set[CharacterStatsUpdate]] =
      allTeams.map(_.members.map(id => CharacterStatsUpdate(id, ladder(id), current(id))))

    val evaluator = new ClusteringEvaluator(List(Feature.const[CharacterStatsUpdate]))
    val buckets: Seq[Set[CharacterStatsUpdate]] = evaluator.splitIntoBuckets(ladderUpdate).toSeq

    buckets should contain theSameElementsAs expectedBuckets
  }

  it should "return empty sequence if there were no updates" in {
    val ladderUpdate = LadderUpdate(ladder, ladder)

    val evaluator = new ClusteringEvaluator(Nil)
    val buckets = evaluator.splitIntoBuckets(ladderUpdate).toSeq

    buckets.size should be(0)
  }

  "noiseFilter" should "leave collection intact for 0" in {
    val current = play(ladder, team1580, team1500)
    val updates = team1580.members.map(id => CharacterStatsUpdate(id, ladder(id), current(id)))

    val evaluator = new ClusteringEvaluator(Nil)

    val filter = evaluator.noiseFilter(0)
    filter(updates) should contain theSameElementsAs updates
  }

  it should "drop required number of elements" in {
    val current = play(ladder, team1580, team1500)
    val updates = (team1500.members ++ team1580.members).map(id => CharacterStatsUpdate(id, ladder(id), current(id)))

    val evaluator = new ClusteringEvaluator(List(Feature.const[CharacterStatsUpdate]))

    val filter = evaluator.noiseFilter(2)
    filter(updates).size should be(4)
  }

  "findTeamsInUpdate" should "return correct teams" in {
    val current = play(ladder, team1580, team1500)
    val ladderUpdate = LadderUpdate(ladder, current)

    val evaluator = new ClusteringEvaluator(List(Feature.const[CharacterStatsUpdate]))
    val teams = evaluator.findTeamsInUpdate(ladderUpdate, RealClusterer.identity)

    teams should contain theSameElementsAs Seq(team1500, team1580)
  }

  "findTeamsInBucket" should "return correct team" in {
    val current = play(ladder, team1580, team1500)
    val updates = team1500.members.map(id => CharacterStatsUpdate(id, ladder(id), current(id)))

    val evaluator = new ClusteringEvaluator(List(Feature.const[CharacterStatsUpdate]))
    val teams = evaluator.findTeamsInBucket(updates, 2, RealClusterer.identity)

    teams should contain theSameElementsAs Seq(team1500)
  }

}
