package info.fotm.clustering

import info.fotm.clustering.RMClustering.{EqClusterer2, EqClusterer}
import info.fotm.clustering.enhancers.{ClonedClusterer, Verifier, Summator, Multiplexer}
import info.fotm.domain.Domain._
import info.fotm.domain._
import info.fotm.util.Statistics.Metrics
import info.fotm.util.{Statistics, MathVector}

object ClusteringEvaluator extends App {
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

  def findTeams(clusterer: RealClusterer, diffs: Seq[CharFeatures], teamSize: Int): Set[Team] = {
    if (diffs.isEmpty)
      Set()
    else {
      val features: Seq[MathVector] = Statistics.normalize(diffs.map(featurize))
      val featureMap = diffs.map(_.prevInfo.id).zip(features).toMap
      val clusters = clusterer.clusterize(featureMap, teamSize)
      clusters.map(ps => Team(ps.toSet))
    }
  }

  def evaluateStep(clusterer: RealClusterer,
                   ladder: LadderSnapshot,
                   nextLadder: LadderSnapshot,
                   games: Set[Game],
                   nLost: Int = 0): Statistics.Metrics = {
    print(".")
    val teamsPlayed: Set[Team] = games.flatMap(g => Seq(g._1, g._2))

    val (wTeams, leTeams) = teamsPlayed.partition(t => t.rating(nextLadder) - t.rating(ladder) > 0)
    val (eTeams, lTeams) = leTeams.partition(t => t.rating(nextLadder) - t.rating(ladder) == 0)

    // algo input: ladder diffs for playersPlayed
    val wDiffs = wTeams.flatMap(_.members).toList.map { p => CharFeatures(ladder(p), nextLadder(p)) }
    val lDiffs = lTeams.flatMap(_.members).toList.map { p => CharFeatures(ladder(p), nextLadder(p)) }
    val eDiffs = eTeams.flatMap(_.members).toList.map { p => CharFeatures(ladder(p), nextLadder(p)) }

    val noisyWDiffs = wDiffs.drop(nLost/2).dropRight(nLost/2)
    val noisyLDiffs = lDiffs.drop(nLost/2).dropRight(nLost/2)

    // algo evaluation: match output against teamsPlayed
    val teamSize = teamsPlayed.head.members.size

    val teams =
      findTeams(clusterer, noisyWDiffs, teamSize) ++
      findTeams(clusterer, noisyLDiffs, teamSize) ++
      findTeams(clusterer, eDiffs, teamSize)

    // noiseless
//    val teams =
//      findTeams(clusterer, wDiffs, teamSize) ++
//        findTeams(clusterer, lDiffs, teamSize) ++
//        findTeams(clusterer, eDiffs, teamSize)

    Statistics.calcMetrics(teams, teamsPlayed)
  }

  def evaluate(clusterer: RealClusterer, data: Seq[(LadderSnapshot, LadderSnapshot, Set[Game])]): Double = {
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
      val data = dataGen.prepareData().drop(500).take(200).toList
      //val (prevLadder, lastladder, _) = data.last
      //lastladder.values.toList.sortBy(-_.rating).map(i => (i.rating, i.seasonWins, i.seasonLosses, i.weeklyWins, i.weeklyLosses)).foreach(println)

      val clusterers: Map[String, RealClusterer] = Map(
//        "Random" -> RealClusterer.wrap(new RandomClusterer),
//        "HTClusterer" -> RealClusterer.wrap(new HTClusterer),
        "HTClusterer2" -> RealClusterer.wrap(new HTClusterer2),
        "HTClusterer2 + Verifier" -> new ClonedClusterer(RealClusterer.wrap(new HTClusterer2)) with Verifier,
        "RMClusterer" -> RealClusterer.wrap(new EqClusterer2),
        "RMClusterer + Verifier" -> new ClonedClusterer(RealClusterer.wrap(new EqClusterer2)) with Verifier,
//        "Closest" -> RealClusterer.wrap(new ClosestClusterer),
//        "Closest * Multiplexer" -> new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer,
        "Closest * Multiplexer * Verifier" -> new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer with Verifier,
        //      "Closest + Verifier" -> new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Verifier,
        //      "HTClusterer + Verifier" -> RealClusterer.wrap(new HTClusterer),
        ////      "HT + RM + Verifier" -> new Summator(RealClusterer.wrap(new EqClusterer), RealClusterer.wrap(new HTClusterer)) with Verifier
        ////      "HT + RM + Closest" -> new Summator(new EqClusterer, new HTClusterer, new ClosestClusterer),
        //      "HT + RM + (Closest with Multiplexer)" -> new Summator(
        //        RealClusterer.wrap(new EqClusterer),
        //        RealClusterer.wrap(new HTClusterer),
        //        new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer
        //      ),
//        "(HT + RM + Closest) * Verifier" -> new ClonedClusterer(new Summator(
//          RealClusterer.wrap(new EqClusterer),
//          RealClusterer.wrap(new HTClusterer),
//          RealClusterer.wrap(new ClosestClusterer)
//        )) with Verifier,
        "(HT2 + Closest * Multiplexer) * Verifier(2)" -> new ClonedClusterer(new Summator(
          RealClusterer.wrap(new HTClusterer2),
          new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer
        )) with Verifier
      )

      for ((name, clusterer) <- clusterers.par) {
        val result = evaluate(clusterer, data)
        println(s"$name = $result")
      }
    }
  }
}
