package info.fotm.clustering

import info.fotm.clustering.RMClustering.EqClusterer2
import info.fotm.clustering.enhancers.{ClonedClusterer, Verifier, Summator, Multiplexer}
import info.fotm.domain.Domain._
import info.fotm.domain._
import info.fotm.util.Statistics.Metrics
import info.fotm.util.{Statistics, MathVector}

object ClusteringEvaluator extends App {
  def sqr(x: Double) = x * x

  def featurize(ci: CharFeatures): MathVector = MathVector(
    ci.next.rating - ci.prev.rating,
    sqr(ci.next.rating - ci.prev.rating) / ci.prev.rating.toDouble,
    ci.next.rating,
    ci.next.season.wins / ci.next.season.total.toDouble,
    ci.next.weekly.wins / ci.next.weekly.total.toDouble,
    ci.next.weekly.total
  )

  def findTeams(clusterer: RealClusterer, diffs: Seq[CharFeatures], teamSize: Int): Set[Team] = {
    if (diffs.isEmpty)
      Set()
    else {
      val features: Seq[MathVector] = Statistics.normalize(diffs.map(featurize))
      val featureMap = diffs.map(_.id).zip(features).toMap
      val clusters = clusterer.clusterize(featureMap, teamSize)
      clusters.map(ps => Team(ps.toSet))
    }
  }

  def evaluateStep(clusterer: RealClusterer,
                   ladder: CharacterLadder,
                   nextLadder: CharacterLadder,
                   games: Set[Game],
                   nLost: Int = 0): Statistics.Metrics = {
    print(".")
    val teamsPlayed: Set[Team] = games.flatMap(g => Seq(g._1, g._2))
    val currentSnapshots = teamsPlayed.map(t => (t, TeamSnapshot(t, ladder))).toMap
    val nextSnapshots = teamsPlayed.map(t => (t, TeamSnapshot(t, nextLadder))).toMap

    val (wTeams, leTeams) = teamsPlayed.partition(t => nextSnapshots(t).rating - currentSnapshots(t).rating > 0)
    val (eTeams, lTeams) = leTeams.partition(t => nextSnapshots(t).rating - currentSnapshots(t).rating == 0)

    // algo input: ladder diffs for playersPlayed
    val wDiffs = wTeams.flatMap(_.members).toList.map { p => CharFeatures(p, ladder(p), nextLadder(p)) }
    val lDiffs = lTeams.flatMap(_.members).toList.map { p => CharFeatures(p, ladder(p), nextLadder(p)) }
    val eDiffs = eTeams.flatMap(_.members).toList.map { p => CharFeatures(p, ladder(p), nextLadder(p)) }

    val noisyWDiffs = wDiffs.drop(nLost/2).dropRight(nLost/2)
    val noisyLDiffs = lDiffs.drop(nLost/2).dropRight(nLost/2)

    // algo evaluation: match output against teamsPlayed
    val teamSize = teamsPlayed.head.members.size
    val teams: Set[Team] =
      findTeams(clusterer, noisyWDiffs, teamSize) ++
      findTeams(clusterer, noisyLDiffs, teamSize) ++
      findTeams(clusterer, eDiffs, teamSize)

    // remove contentions (penalize multiplexer and merged algos)
    val characters = teams.flatMap(t => t.members).toList
    val charTeams: Map[CharacterId, Set[Team]] = characters.map(c => (c, teams.filter(t => t.members.contains(c)))).toMap
    val (certain, _) = charTeams.partition(kv => kv._2.size == 1)

    // noiseless
//    val teams =
//      findTeams(clusterer, wDiffs, teamSize) ++
//        findTeams(clusterer, lDiffs, teamSize) ++
//        findTeams(clusterer, eDiffs, teamSize)

    val ct = certain.values.flatten.toSet
    Statistics.calcMetrics(ct, teamsPlayed)
  }

  def evaluate(clusterer: RealClusterer, data: Seq[(CharacterLadder, CharacterLadder, Set[Game])]): Double = {
    val stats: Seq[Metrics] =
      for {
        (ladder, nextLadder, games) <- data
        noise = 2 * games.head._1.members.size - 1
      }
      yield evaluateStep(clusterer, ladder, nextLadder, games, noise)

    val combinedMetrics: Metrics = stats.reduce(_ + _)
    println(s"\n$combinedMetrics")

    Statistics.fScore(0.5)(combinedMetrics)
  }

  {
    for ((_, settings) <- Defaults.settings) {
      println(s"Evaluating $settings:")
      val dataGen = new ClusteringEvaluatorData(settings)

      // Runner
      val data = dataGen.prepareData().slice(500, 700).toList
      //val (prevLadder, lastladder, _) = data.last
      //lastladder.values.toList.sortBy(-_.rating).map(i => (i.rating, i.seasonWins, i.seasonLosses, i.weeklyWins, i.weeklyLosses)).foreach(println)

      def createCMV(turns: Int, threshold: Int): (String, RealClusterer) = {
        s"C * M($turns, $threshold) * V" ->
          new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer with Verifier {
            override lazy val multiplexTurns = turns
            override lazy val multiplexThreshold = threshold
          }
      }

      val clusterers: Map[String, RealClusterer] = Map(
//        "Random" -> RealClusterer.wrap(new RandomClusterer),
        "HT2" -> RealClusterer.wrap(new HTClusterer2),
        "HT3" -> RealClusterer.wrap(new HTClusterer3),
        "HT2 * V" -> new ClonedClusterer(RealClusterer.wrap(new HTClusterer2)) with Verifier,
        "HT3 * V" -> new ClonedClusterer(RealClusterer.wrap(new HTClusterer3)) with Verifier,
        "HT3[RM]" -> RealClusterer.wrap(new HTClusterer3(Some(new EqClusterer2))),
        "HT3[RM] * V" -> new ClonedClusterer(RealClusterer.wrap(new HTClusterer3(Some(new EqClusterer2)))) with Verifier,
        "RM" -> RealClusterer.wrap(new EqClusterer2),
        "RM * V" -> new ClonedClusterer(RealClusterer.wrap(new EqClusterer2)) with Verifier,
        createCMV(20, 3),
        "(HT3 + CM) * V" -> new Summator(
          RealClusterer.wrap(new HTClusterer3),
          new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer
        ) with Verifier,
        "(HT3[RM] + CM) * V" -> new Summator(
          RealClusterer.wrap(new HTClusterer3(Some(new EqClusterer2))),
          new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer
        ) with Verifier
      )

      for ((name, clusterer) <- clusterers.par) {
        val result = evaluate(clusterer, data)
        println(s"$name = $result")
      }
    }
  }
}
