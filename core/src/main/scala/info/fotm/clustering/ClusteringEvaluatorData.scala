package info.fotm.clustering

import info.fotm.domain.Domain._
import info.fotm.domain._

import scala.util.Random

object ClusteringEvaluatorData {
  lazy val matchesPerTurn = 5
  lazy val rng = new Random()
  lazy val ladderSize = 500
  lazy val teamSize = 3

  def genPlayer = {
    val id = java.util.UUID.randomUUID().toString
    CharacterStats(CharacterId(id), 1500, 0, 0, 0, 0)
  }

  def genLadder(nPlayers: Int): LadderSnapshot = (0 until nPlayers).map(i => {
    val player = genPlayer
    (player.id, player)
  }).toMap

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

    lTeam.members.foldLeft(ladderWithWinners) { (ladder, charId) =>
      val ratingDelta = calcRating(charId, wTeam, won = false)(currentLadder)
      val charInfo = currentLadder(charId)

      val newCharInfo = charInfo.copy(
        rating = ratingDelta,
        weeklyLosses = charInfo.weeklyLosses + 1,
        seasonLosses = charInfo.seasonLosses + 1)

      ladder.updated(charInfo.id, newCharInfo)
    }
  }

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

  lazy val gamesPerWeek = 50

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
}
