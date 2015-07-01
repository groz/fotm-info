package info.fotm.clustering

import info.fotm.domain.Domain._
import info.fotm.domain._
import info.fotm.util.Statistics.Metrics
import info.fotm.util.{Statistics, MathVector}

object ClusteringEvaluator extends App {
  type TeamFinder = Seq[CharFeatures] => Set[Team]
  def sqr(x: Double) = x * x

  def featurize(ci: CharFeatures): MathVector = MathVector(
    ci.nextInfo.rating - ci.prevInfo.rating,
    sqr(ci.nextInfo.rating - ci.prevInfo.rating),
    sqr(ci.nextInfo.rating - ci.prevInfo.rating) / ci.prevInfo.rating,
    ci.nextInfo.rating,
    ci.nextInfo.seasonWins,
    ci.nextInfo.seasonLosses,
    ci.nextInfo.weeklyWins,
    ci.nextInfo.weeklyLosses
  )

  def findTeams(clusterize: Seq[MathVector] => Set[Seq[MathVector]], diffs: Seq[CharFeatures]): Set[Team] = {
    // TODO: ATTENTION! Works only as long as MathVector is compared by ref
    //val featurizedDiffs = Statistics.normalize( diffs.map(featurize) )
    val featurizedDiffs = diffs.map(featurize)

    val featureMap: Map[MathVector, CharacterId] = featurizedDiffs.zip(diffs.map(_.prevInfo.id)).toMap

    val clusters: Set[Seq[MathVector]] = clusterize(featurizedDiffs)
    clusters.map(cluster => Team(cluster.map(featureMap).toSet))
  }

  def evaluateStep(findTeams: TeamFinder,
                   ladder: LadderSnapshot,
                   nextLadder: LadderSnapshot,
                   games: Set[Game]): Statistics.Metrics = {
    val teamsPlayed: Set[Team] = games.map(g => Seq(g._1, g._2)).flatten
    val playersPlayed: Set[CharacterId] = teamsPlayed.flatMap(_.members)

    // algo input: ladder diffs for playersPlayed
    val (wDiffs: List[CharFeatures], lDiffs: List[CharFeatures]) = playersPlayed.toList
      .map { p => CharFeatures(ladder(p), nextLadder(p)) }
      .partition(d => d.nextInfo.rating - d.prevInfo.rating > 0)

    // algo evaluation: match output against teamsPlayed
    val teams = findTeams(wDiffs) ++ findTeams(lDiffs)
    Statistics.calcMetrics(teams, teamsPlayed)
  }

  def evaluate(findTeams: TeamFinder, data: Seq[(LadderSnapshot, LadderSnapshot, Set[Game])]): Double = {
    val stats: Seq[Metrics] =
      for { (ladder, nextLadder, games) <- data }
      yield evaluateStep(findTeams, ladder, nextLadder, games)

    val combinedMetrics: Metrics = stats.reduce(_ + _)
    println(combinedMetrics)

    Statistics.f1Score(combinedMetrics)
  }

  // Runner
  import ClusteringEvaluatorData._

  val data = prepareData().drop(200).take(100).toList
  //val (prevLadder, lastladder, _) = data.last
  //lastladder.values.toList.sortBy(-_.rating).map(i => (i.rating, i.seasonWins, i.seasonLosses)).foreach(println)

  val clusterers = Map(
    "HTClusterer" -> new HTClusterer,
    "ClosestClusterer" -> new ClosestClusterer
  )

  val finders: Map[String, TeamFinder] = clusterers.map { kv =>
    val (name, clusterer) = kv

    def teamFinder(diffs: Seq[CharFeatures]): Set[Team] =
      findTeams(ps => clusterer.clusterize(ps, teamSize), diffs)

    (name, teamFinder _)
  }

  for ((name, finder) <- finders) {
    val result = evaluate(finder, data)
    println(s"$name = $result")
  }
}
