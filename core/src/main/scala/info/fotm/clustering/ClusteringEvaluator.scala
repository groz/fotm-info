package info.fotm.clustering

import info.fotm.domain.Domain.LadderSnapshot
import info.fotm.domain.{Team, CharacterStats, CharacterId}
import info.fotm.util.{Statistics, MathVector}
import scala.util.Random

object ClusteringEvaluator extends App {

  lazy val ladderSize = 500
  lazy val teamSize = 3
  lazy val matchesPerTurn = 20
  lazy val rng = new Random()

  def featurize(ci: CharFeatures): MathVector = MathVector(
    ci.nextInfo.rating - ci.prevInfo.rating,
    ci.nextInfo.rating,
    ci.nextInfo.seasonWins,
    ci.nextInfo.seasonLosses,
    ci.nextInfo.weeklyWins,
    ci.nextInfo.weeklyLosses
  ).normalize

  def genPlayer = {
    val id = java.util.UUID.randomUUID().toString
    CharacterStats(CharacterId(id), 1500, 0, 0, 0, 0)
  }

  def genLadder(nPlayers: Int): LadderSnapshot = (0 until nPlayers).map(i => {
    val player = genPlayer
    (player.id, player)
  }).toMap

  type Game = (Team, Team)

  def evaluate(clusterize: Seq[MathVector] => Set[Seq[MathVector]],
               data: Seq[(LadderSnapshot, LadderSnapshot, Set[Game])]): Double = {
    val stats = for {
      (ladder, nextLadder, matches) <- data
    } yield {
      val teamsPlayed: Set[Team] = matches.map(m => Seq(m._1, m._2)).flatten
      val playersPlayed: Set[CharacterId] = teamsPlayed.flatMap(_.members)

      // TODO: ATTENTION! Works only as long as MathVector is compared by ref
      // algo input: ladder diffs for playersPlayed
      val diffs: List[CharFeatures] = playersPlayed.toList.map { p => CharFeatures(ladder(p), nextLadder(p)) }
      val featurizedDiffs: List[MathVector] = diffs.map(featurize)
      val featureMap: Map[MathVector, CharacterId] = featurizedDiffs.zip(diffs.map(_.prevInfo.id)).toMap

      // algo output: players grouped into teams
      val algoOutputClusters: Set[Seq[MathVector]] = clusterize(featurizedDiffs)
      val algoOutputTeams: Set[Team] = algoOutputClusters.map(cluster => Team(cluster.map(featureMap).toSet))

      // algo evaluation: match output against teamsPlayed
      val guessed: Set[Team] = teamsPlayed.intersect(algoOutputTeams)   // true positive
      val misguessed: Set[Team] = algoOutputTeams -- teamsPlayed        // false positive
      val missed: Set[Team] = teamsPlayed -- algoOutputTeams            // false negative
      val result = (guessed.size, misguessed.size, missed.size)
      println(result)
      result
    }

    val (tp, fp, fn) = stats.foldLeft((0, 0, 0)) { (acc, s) => (acc._1 + s._1, acc._2 + s._2, acc._3 + s._3) }
    Statistics.f1Score(tp, fp, fn)
  }

  // selects pairs of (winner, loser) from teams
  def pickGamesRandomly(ladder: LadderSnapshot, teams: Seq[Team]): Seq[(Team, Team)] = {
    val shuffledTeams: Seq[Team] = rng.shuffle(teams)
    shuffledTeams
      .sliding(2, 2)
      .map { s =>
        val team1 = s(0)
        val team2 = s(1)
        if (rng.nextDouble() < winChance(team1.rating(ladder), team2.rating(ladder)))
          (team1, team2)
        else
          (team2, team1)
      }
      .take(matchesPerTurn)
      .toSeq
  }

  def replacePlayer(team: Team, src: CharacterId, dst: CharacterId) = team.copy(team.members - src + dst)

  def hopTeams(team1: Team, team2: Team): (Team, Team) = {
    val p1 = rng.shuffle(team1.members.toList).head
    val p2 = rng.shuffle(team2.members.toList).head
    (replacePlayer(team1, p1, p2), replacePlayer(team2, p2, p1))
  }

  def hopTeamsRandomly(teams: Seq[Team]): Seq[Team] = {
    val hopRatio = 0.1
    val nHopTeams: Int = (teams.size * hopRatio).toInt

    val shuffledTeams = rng.shuffle(teams.toList)
    val teams1 = shuffledTeams.take(nHopTeams)
    val teams2 = shuffledTeams.drop(nHopTeams).take(nHopTeams)
    val rest = shuffledTeams.drop(2 * nHopTeams)

    val hopped = for {
      (t1, t2) <- teams1.zip(teams2)
    } yield {
      val (r1, r2) = hopTeams(t1, t2)
      Seq(r1, r2)
    }

    hopped.flatten ++ rest
  }

  def prepareData(ladderOpt: Option[LadderSnapshot] = None,
                  teamsOpt: Option[Seq[Team]] = None,
                  hopTeams: Seq[Team] => Seq[Team] = hopTeamsRandomly,
                  pickGames: (LadderSnapshot, Seq[Team]) => Seq[(Team, Team)] = pickGamesRandomly)
                  : Stream[(LadderSnapshot, LadderSnapshot, Set[Game])] = {
    val ladder: LadderSnapshot = ladderOpt.getOrElse( genLadder(ladderSize) )

    val prevTeams: Seq[Team] = teamsOpt.getOrElse {
      val ids = ladder.keys.toSeq
      val result = ids.sliding(teamSize, teamSize).map(p => Team(p.toSet)).toSeq
      if (result.last.members.size == teamSize) result else result.init
    }

    val teams = hopTeams(prevTeams)

    val matches: Seq[(Team, Team)] = pickGames(ladder, teams)

    val nextLadder: LadderSnapshot = matches.foldLeft(ladder) { (l, teams) => play(l, teams._1, teams._2) }

    (ladder, nextLadder, matches.toSet) #:: prepareData(Some(nextLadder), Some(teams), hopTeams, pickGames)
  }

  def calcRating(charId: CharacterId, team: Team, won: Boolean)(ladder: LadderSnapshot): Int = {
    val teamRating = team.rating(ladder)
    val charInfo = ladder(charId)
    if (won) {
      charInfo.rating + calcRatingChange(charInfo.rating, teamRating)
    } else {
      charInfo.rating - calcRatingChange(teamRating, charInfo.rating)
    }
  }

  def winChance(winnerRating: Double, loserRating: Double): Double =
    1.0 / (1.0 + Math.pow(10, (loserRating - winnerRating)/400.0))

  def calcRatingChange(winnerRating: Double, loserRating: Double): Int = {
    val chance = winChance(winnerRating, loserRating)
    val k = 32
    Math.round(k * (1 - chance)).toInt
  }

  def play(currentLadder: LadderSnapshot, wTeam: Team, lTeam: Team): LadderSnapshot = {
    val ladderWithWinners: LadderSnapshot =
      wTeam.members.foldLeft(currentLadder) { (ladder, charId) =>
        val ratingDelta = calcRating(charId, lTeam, won = true)(currentLadder)
        val charInfo = currentLadder(charId)

        val newCharInfo = charInfo.copy(
          rating = ratingDelta,
          weeklyWins = charInfo.weeklyWins + 1,
          seasonWins = charInfo.seasonWins + 1)

        ladder.updated(charInfo.id, newCharInfo)
      }

    val result: LadderSnapshot =
      lTeam.members.foldLeft(ladderWithWinners) { (ladder, charId) =>
        val ratingDelta = calcRating(charId, wTeam, won = false)(currentLadder)
        val charInfo = currentLadder(charId)

        val newCharInfo = charInfo.copy(
          rating = ratingDelta,
          weeklyLosses = charInfo.weeklyLosses + 1,
          seasonLosses = charInfo.seasonLosses + 1)

        ladder.updated(charInfo.id, newCharInfo)
      }

    result
  }

  val data = prepareData().drop(200).take(100).toList
  val (prevLadder, lastladder, _) = data.last
  lastladder.values.toList.sortBy(-_.rating).map(i => (i.rating, i.seasonWins, i.seasonLosses)).foreach(println)

  val clusterer = new HTClusterer

  var i = 0
  val htClusterize = { (ps: Seq[MathVector]) =>
    println(i)
    i += 1
    clusterer.clusterize(ps, teamSize)
  }

  val htResult = evaluate(htClusterize, data)
  println(s"HT result: $htResult")
}
