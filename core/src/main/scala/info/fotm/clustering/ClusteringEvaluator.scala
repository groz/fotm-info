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

    val (wTeams, leTeams) = teamsPlayed.partition(t => t.rating(nextLadder) - t.rating(prevLadder) > 0)
    val (eTeams, lTeams) = leTeams.partition(t => t.rating(nextLadder) - t.rating(prevLadder) == 0)

    // algo input: ladder diffs for playersPlayed
    val wDiffs = wTeams.flatMap(_.members).toList.map { p => CharFeatures(ladder(p), nextLadder(p)) }
    val lDiffs = lTeams.flatMap(_.members).toList.map { p => CharFeatures(ladder(p), nextLadder(p)) }
    val eDiffs = eTeams.flatMap(_.members).toList.map { p => CharFeatures(ladder(p), nextLadder(p)) }

    // algo evaluation: match output against teamsPlayed
    //println(wDiffs.size, lDiffs.size)
    val teams = findTeams(wDiffs) ++ findTeams(lDiffs) ++ findTeams(eDiffs)
    Statistics.calcMetrics(teams, teamsPlayed)
  }

  def evaluate(findTeams: TeamFinder, data: Seq[(LadderSnapshot, LadderSnapshot, Set[Game])]): Double = {
    val stats: Seq[Metrics] =
      for { (ladder, nextLadder, games) <- data }
      yield evaluateStep(findTeams, ladder, nextLadder, games)

    val combinedMetrics: Metrics = stats.reduce(_ + _)
    println(s"\n$combinedMetrics")

    Statistics.f1Score(combinedMetrics)
  }

  // Runner
  import ClusteringEvaluatorData._

  val data = prepareData().drop(500).take(1000).toList
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
        if (ps.size == 0) Set(ps)
        else clusterer.clusterize(ps, teamSize)
      }, diffs)
    }

    (name, teamFinder _)
  }

  {
    //val tmp = List(MathVector(0.5555555555555556,0.3287540435759616,0.04710144927536232,0.32075471698113206,1.0,0.0,0.0), MathVector(0.5555555555555556,0.3251508445255977,0.09420289855072464,0.09433962264150944,0.7818181818181819,0.0,0.0), MathVector(0.5555555555555556,0.28820126329503504,0.6376811594202898,0.4716981132075472,0.6909090909090909,0.0,0.0), MathVector(0.3333333333333333,0.5664491826728949,0.391304347826087,0.24528301886792453,0.7272727272727273,0.0,0.0), MathVector(0.0,1.0,0.8731884057971014,0.07547169811320754,0.6909090909090909,0.0,0.0), MathVector(0.9444444444444444,0.019237967780170197,0.7862318840579711,0.3018867924528302,0.5636363636363636,1.0,0.0), MathVector(0.8888888888888888,0.04212266908841039,0.8731884057971014,0.4528301886792453,0.45454545454545453,1.0,0.0), MathVector(0.8888888888888888,0.04331551356678264,0.7898550724637681,0.4339622641509434,0.5272727272727272,1.0,0.0), MathVector(0.7777777777777778,0.1202758220835831,0.2427536231884058,0.39622641509433965,0.8,0.0,0.0), MathVector(0.5,0.34033110390884697,0.6920289855072463,0.22641509433962265,0.7636363636363637,0.0,0.0), MathVector(0.2777777777777778,0.634505132881742,0.4601449275362319,0.0,0.8,0.0,0.0), MathVector(0.6111111111111112,0.27386783451856617,0.0,0.37735849056603776,0.6363636363636364,0.0,0.0), MathVector(0.8333333333333334,0.0677885842149685,1.0,1.0,0.0,0.0,0.0), MathVector(1.0,0.0,0.7065217391304348,0.8679245283018868,0.32727272727272727,0.0,0.0), MathVector(0.9444444444444444,0.019458844002299074,0.7644927536231884,0.7924528301886793,0.09090909090909091,0.0,0.0))
    //clusterers("RMClusterer").clusterize(tmp, 3)
    //println("ok")
  }

  for ((name, finder) <- finders) {
    val result = evaluate(finder, data)
    println(s"$name = $result")
  }
}
