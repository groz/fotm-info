package info.fotm.clustering

import info.fotm.api.models.LeaderboardRow
import info.fotm.domain._

import scala.util.Random

object ClusteringEvaluatorData {
  type DataPoint = (CharacterLadder, CharacterLadder, Set[(Team, Team)]) // previous, current, teamsPlayed
  type DataSet = List[DataPoint]
}

class ClusteringEvaluatorData(settings: EvaluatorSettings = Defaults.settings(2)) {
  import settings._
  import ClusteringEvaluatorData._

  lazy val turnsPerWeek = 7 * 2
  lazy val rng = new Random()

  def genPlayer: CharacterSnapshot = genPlayer(1500)

  def genPlayer(rating: Int): CharacterSnapshot = {
    val id = java.util.UUID.randomUUID().toString
    val raw = LeaderboardRow(
      ranking = 1,
      rating = rating,
      name = id,
      realmId = 1,
      realmName = "123",
      realmSlug = "123",
      raceId = 1,
      classId = 1,
      specId = 1,
      factionId = 1,
      genderId = 1,
      seasonWins = 0,
      seasonLosses = 0,
      weeklyWins = 0,
      weeklyLosses = 0
    )
    CharacterSnapshot(raw)
  }

  def genLadder(nPlayers: Int): CharacterLadder = {
    val rows = (0 until nPlayers).map(i => genPlayer).map(c => (c.id, c)).toMap
    CharacterLadder(Axis.all.head, rows)
  }

  // selects pairs of (winner, loser) from teams
  def pickGamesRandomly(ladder: CharacterLadder, teams: Seq[Team]): Seq[(Team, Team)] = {
    val shuffledTeams: Seq[Team] = rng.shuffle(teams)
    shuffledTeams
      .sliding(2, 2)
      .map { s =>
        val (team1, team2) = (s(0), s(1))
        val (rating1, rating2) = (TeamSnapshot(team1, ladder).rating, TeamSnapshot(team2, ladder).rating)
        if (rng.nextDouble() < winChance(rating1, rating2))
          (team1, team2)
        else
          (team2, team1)
      }
      .take(matchesPerTurn)
      .toSeq
  }

  def replacePlayer(team: Team, src: CharacterId, dst: CharacterId): Team = {
    if (team.members.contains(src) && team.members.contains(dst)) team
    else team.copy(team.members - src + dst)
  }

  def calcRating(charId: CharacterId, team: Team, won: Boolean)(ladder: CharacterLadder): Int = {
    val teamRating = TeamSnapshot(team, ladder).rating
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

  def play(ladder: CharacterLadder, wTeam: Team, lTeam: Team): CharacterLadder = {
    val ladderWithWinners: CharacterLadder =
      wTeam.members.foldLeft(ladder) { (ladder, charId) =>
        val rating = calcRating(charId, lTeam, won = true)(ladder)
        val snapshot: CharacterSnapshot = ladder.rows(charId)

        val updatedStats = CharacterStats(rating, snapshot.stats.weekly.win, snapshot.stats.season.win)
        ladder.copy(rows = ladder.rows.updated(charId, snapshot.copy(stats = updatedStats)))
      }

    lTeam.members.foldLeft(ladderWithWinners) { (ladder, charId) =>
      val rating = calcRating(charId, wTeam, won = false)(ladder)
      val snapshot: CharacterSnapshot = ladder.rows(charId)

      val updatedStats = CharacterStats(rating, snapshot.stats.weekly.loss, snapshot.stats.season.loss)
      ladder.copy(rows = ladder.rows.updated(charId, snapshot.copy(stats = updatedStats)))
    }
  }

  def hopTeams(team1: Team, team2: Team): Seq[Team] = {
    val p1 = rng.shuffle(team1.members.toList).head
    val p2 = rng.shuffle(team2.members.toList).head
    Seq(replacePlayer(team1, p1, p2), replacePlayer(team2, p2, p1))
  }

  def hopTeamsRandomly(teams: Seq[Team], ladder: CharacterLadder, hopRatioOpt: Option[Double] = None): Seq[Team] = {
    val hr = hopRatioOpt.getOrElse(hopRatio)

    val pairs = for {
      window: Seq[Team] <- teams.sortBy(t => TeamSnapshot(t, ladder).rating).sliding(2, 2)
      t1 = window.head
      t2 = window.last
    } yield {
      if (t1 == t2)
        Seq(t1)
      else if (rng.nextDouble < hr)
        hopTeams(t1, t2)
      else
        Seq(t1, t2)
    }

    pairs.flatten.toSeq
  }

  def resetWeeklyStats(ladder: CharacterLadder): CharacterLadder = {
    val newRows = for {
      (id, charSnapshot) <- ladder.rows
    } yield {
        val newStats = charSnapshot.stats.copy(weekly = Stats.empty)
        val newSnapshot = charSnapshot.copy(stats = newStats)
        (id, newSnapshot)
      }
    ladder.copy(rows = newRows)
  }

  def updatesStream(
      ladderOpt: Option[CharacterLadder] = None,
      teamsOpt: Option[Seq[Team]] = None,
      hopTeams: (Seq[Team], CharacterLadder) => Seq[Team] = hopTeamsRandomly(_, _, None),
      pickGames: (CharacterLadder, Seq[Team]) => Seq[(Team, Team)] = pickGamesRandomly,
      i: Int = 0)
    : Stream[DataPoint] = {

    val ladderInput: CharacterLadder = ladderOpt.getOrElse( genLadder(ladderSize) )

    val ladder: CharacterLadder =
      if (i % turnsPerWeek != 0) ladderInput
      else resetWeeklyStats(ladderInput)

    val prevTeams: Seq[Team] = teamsOpt.getOrElse {
      val ids = ladder.rows.keys.toSeq
      val result = ids.sliding(teamSize, teamSize).map(p => Team(p.toSet)).toSeq
      if (result.last.members.size == teamSize) result else result.init
    }

    val teams = hopTeams(prevTeams, ladder)

    val matches: Seq[(Team, Team)] = pickGames(ladder, teams)

    val nextLadder: CharacterLadder = matches.foldLeft(ladder) { (l, t) => play(l, t._1, t._2) }

    (ladder, nextLadder, matches.toSet) #:: updatesStream(Some(nextLadder), Some(teams), hopTeams, pickGames, i + 1)
  }
}
