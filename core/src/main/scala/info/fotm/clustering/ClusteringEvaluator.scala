package info.fotm.clustering

import info.fotm.clustering.RMClustering.EqClusterer
import info.fotm.domain.Domain._
import info.fotm.domain._
import info.fotm.util.Statistics.Metrics
import info.fotm.util.{Statistics, MathVector}

object ClusteringEvaluator extends App {
  type TeamFinder = Seq[CharFeatures] => Set[Team]
  def sqr(x: Double) = x * x

  def featurize(ci: CharFeatures): MathVector = MathVector(
    ci.nextInfo.rating - ci.prevInfo.rating,
    sqr(ci.nextInfo.rating - ci.prevInfo.rating) / ci.prevInfo.rating.toDouble,
    ci.nextInfo.rating,
    ci.nextInfo.seasonWins,
    ci.nextInfo.seasonLosses,
    ci.nextInfo.weeklyWins,
    ci.nextInfo.weeklyLosses
  )

  def findTeams(clusterize: Seq[MathVector] => Set[Seq[MathVector]], diffs: Seq[CharFeatures]): Set[Team] = {
    // TODO: ATTENTION! Works only as long as MathVector is compared by ref
    val featurizedDiffs = Statistics.normalize( diffs.map(featurize) )
    //val featurizedDiffs = diffs.map(featurize)

    val featureMap: Map[MathVector, CharacterId] = featurizedDiffs.zip(diffs.map(_.prevInfo.id)).toMap

    val clusters: Set[Seq[MathVector]] = clusterize(featurizedDiffs)
    clusters.map(cluster => Team(cluster.map(featureMap).toSet))
  }

  def evaluateStep(findTeams: TeamFinder,
                   ladder: LadderSnapshot,
                   nextLadder: LadderSnapshot,
                   games: Set[Game]): Statistics.Metrics = {
    val teamsPlayed: Set[Team] = games.map(g => Seq(g._1, g._2)).flatten

    val (wTeams, leTeams) = teamsPlayed.partition(t => t.rating(nextLadder) - t.rating(ladder) > 0)
    val (eTeams, lTeams) = leTeams.partition(t => t.rating(nextLadder) - t.rating(ladder) == 0)

    // algo input: ladder diffs for playersPlayed
    val wDiffs = wTeams.flatMap(_.members).toList.map { p => CharFeatures(ladder(p), nextLadder(p)) }
    val lDiffs = lTeams.flatMap(_.members).toList.map { p => CharFeatures(ladder(p), nextLadder(p)) }
    val eDiffs = eTeams.flatMap(_.members).toList.map { p => CharFeatures(ladder(p), nextLadder(p)) }

    // algo evaluation: match output against teamsPlayed
    val teams = findTeams(wDiffs) ++ findTeams(lDiffs) ++ findTeams(eDiffs)
    Statistics.calcMetrics(teams, teamsPlayed)
  }

  def evaluate(findTeams: TeamFinder, data: Seq[(LadderSnapshot, LadderSnapshot, Set[Game])]): Double = {
    val stats: Seq[Metrics] =
      for { (ladder, nextLadder, games) <- data }
      yield evaluateStep(findTeams, ladder, nextLadder, games)

    val combinedMetrics: Metrics = stats.reduce(_ + _)
    println(s"\n$combinedMetrics")

    Statistics.fScore(2)(combinedMetrics)
  }

  {
    // Runner
    import ClusteringEvaluatorData._

    val data = prepareData().drop(500).take(200).toList
    val (prevLadder, lastladder, _) = data.last
    lastladder.values.toList.sortBy(-_.rating).map(i => (i.rating, i.seasonWins, i.seasonLosses, i.weeklyWins, i.weeklyLosses)).foreach(println)

    val clusterers = Map(
      "Random" -> new RandomClusterer,
      "HTClusterer" -> new HTClusterer,
      "ClosestClusterer" -> new ClosestClusterer,
      "RMClusterer" -> new EqClusterer
    )

    val finders: Map[String, TeamFinder] = clusterers.map { kv =>
      val (name, clusterer) = kv

      def teamFinder(diffs: Seq[CharFeatures]): Set[Team] = {
        print('.')

        findTeams((ps: Seq[MathVector]) => {
          /*
          Temporary output to see the data fed into clusterizers

          val inputs = ps.sliding(teamSize, teamSize).toList
          inputs.foreach(println)
          println("==========")
          */

          if (ps.size == 0) Set()
          else clusterer.clusterize(rng.shuffle(ps), teamSize)
        }, diffs)
      }

      (name, teamFinder _)
    }

    for ((name, finder) <- finders) {
      val result = evaluate(finder, data)
      println(s"$name = $result")
    }
  }
}
