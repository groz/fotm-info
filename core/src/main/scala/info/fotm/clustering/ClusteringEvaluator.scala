package info.fotm.clustering

import info.fotm.clustering.ClusteringEvaluatorData.DataPoint
import info.fotm.domain.Domain._
import info.fotm.domain._
import info.fotm.util.Statistics.Metrics
import info.fotm.util.{MathVector, Statistics}

import scala.collection.breakOut
import scala.collection.immutable.Iterable
import scala.util.Random

class ClusteringEvaluator(features: List[Feature[CharacterStatsUpdate]]) extends App {

  type Bucket = Set[CharacterStatsUpdate]
  type BucketFilter = (Bucket => Bucket)

  def findTeamsInUpdate(ladderUpdate: LadderUpdate, clusterer: RealClusterer, bucketFilter: BucketFilter = identity): Set[Team] =
    (for {
      bucket <- splitIntoBuckets(ladderUpdate)
      team <- findTeamsInBucket(bucket, ladderUpdate.current.axis.bracket.size, clusterer, bucketFilter)
    } yield team)(breakOut)

  def splitIntoBuckets(ladderUpdate: LadderUpdate): Iterable[Set[CharacterStatsUpdate]] =
    for {
      (_, factionUpdates) <- ladderUpdate.statsUpdates.groupBy(u => ladderUpdate.current.rows(u.id).view.factionId)
      (winners, losers) = factionUpdates.partition(u => ladderUpdate.current(u.id).season.wins > ladderUpdate.previous(u.id).season.wins)
      bucket <- Seq(winners, losers)
    } yield bucket

  def findTeamsInBucket(inputBucket: Set[CharacterStatsUpdate], teamSize: Int, clusterer: RealClusterer, bucketFilter: BucketFilter = identity): Set[Team] = {
    val bucket = bucketFilter(inputBucket).toSeq // NB! do not remove .toSeq here or .zip below won't work

    if (bucket.isEmpty) Set()
    else {
      val featureVectors: Seq[MathVector] = Feature.normalize(features, bucket)
      val featureMap: Map[CharacterId, MathVector] = bucket.map(_.id).zip(featureVectors)(breakOut)
      val clusters = clusterer.clusterize(featureMap, teamSize)
      clusters.map(ps => Team(ps.toSet))
    }
  }

  def noiseFilter(nLost: Int): BucketFilter =
    bucket => Random.shuffle(bucket.toSeq).drop(nLost / 2).dropRight(nLost / 2).toSet

  def evaluateStep(clusterer: RealClusterer,
                   ladderUpdate: LadderUpdate,
                   games: Set[Game],
                   nLost: Int = 0): Statistics.Metrics = {
    print(".")

    val actualTeamsPlayed: Set[Team] = games.flatMap(g => Seq(g._1, g._2))
    val bucketFilter: BucketFilter = noiseFilter(nLost)
    val teamsFound: Set[Team] = findTeamsInUpdate(ladderUpdate, clusterer, bucketFilter)

    // remove overlapping teams (penalize multiplexer and merged algos?)
    val uncontended: Set[Team] = teamsFound.filter(t => teamsFound.count(_.members.intersect(t.members).nonEmpty) == 1)
    Statistics.calcMetrics(teamsFound, actualTeamsPlayed) // TODO: add noise filtering
  }

  def evaluate(clusterer: RealClusterer, data: Stream[DataPoint]): Double = {
    val stats: Seq[Metrics] = for {
      (ladderUpdate, games) <- data
      noise = 2 * games.head._1.members.size - 1
    } yield evaluateStep(clusterer, ladderUpdate, games, noise)

    val combinedMetrics: Metrics = stats.reduce(_ + _)
    println(s"\n$combinedMetrics")

    Statistics.fScore(0.5)(combinedMetrics)
  }
}
