import info.fotm.clustering._
import ClusteringEvaluatorData._
import ClusteringEvaluator._
import info.fotm.domain.Domain.LadderSnapshot
import info.fotm.domain.Team
import info.fotm.util.MathVector
import org.scalatest._

import scala.collection.immutable.{TreeMap, IndexedSeq}

class ClusteringEvaluatorSpec extends FlatSpec with Matchers with ClusteringEvaluatorSpecBase {
  "evaluateStep" should "return correct metrics when everything is guessed right" in {
    val games = Set((team1580, team1500))

    val nextLadder = play(ladder, team1580, team1500)

    def findTeams(diffs: Seq[CharFeatures]): Set[Team] = Set(team1580, team1500)

    val metrics = evaluateStep(findTeams, ladder, nextLadder, games)

    metrics.truePositive should be(2)
    metrics.falsePositive should be(0)
    metrics.falseNegative should be(0)
  }
}
