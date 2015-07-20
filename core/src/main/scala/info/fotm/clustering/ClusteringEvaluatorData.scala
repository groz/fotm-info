package info.fotm.clustering

import info.fotm.api.models.LeaderboardRow
import info.fotm.domain.Domain.Game
import info.fotm.domain._
import scala.collection.generic.CanBuildFrom
import scala.collection.{GenSeq, GenIterable, breakOut}
import scala.collection.immutable.IndexedSeq

import scala.util.Random

object ClusteringEvaluatorData {
  type DataPoint = (LadderUpdate, Set[Game])
  // previous, current, teamsPlayed
  type DataSet = List[DataPoint]
}

class ClusteringEvaluatorData(settings: EvaluatorSettings = Defaults.settings(3)) {

  import settings._
  import ClusteringEvaluatorData._

  lazy val rng = new Random()

  def genPlayer(rating: Int = 1500, factionId: Int = 1): CharacterSnapshot = {
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
      factionId = factionId,
      genderId = 1,
      seasonWins = 0,
      seasonLosses = 0,
      weeklyWins = 0,
      weeklyLosses = 0
    )
    CharacterSnapshot(raw)
  }

  val axis = Axis.all.find(a => a.bracket.size == settings.teamSize).get

  def genLadder(nPlayers: Int): CharacterLadder = {
    val rows = (0 until nPlayers).map(i => genPlayer()).map(c => (c.id, c)).toMap
    CharacterLadder(axis, rows)
  }

  def pickGamesFromBuckets(teamsWithRatings: IndexedSeq[(Team, Double)]): Set[(Team, Team)] = {

    val shuffled = rng.shuffle(teamsWithRatings.indices.toIndexedSeq)
    val indices = shuffled.take(matchesPerTurn * 2).sorted

    indices
      .grouped(2)
      .map { (s: Seq[Int]) =>
        val ((team1, rating1), (team2, rating2)) = (teamsWithRatings(s.head), teamsWithRatings(s.last))
        if (rng.nextDouble() < winChance(rating1, rating2))
          (team1, team2)
        else
          (team2, team1)
      }
      .take(matchesPerTurn)
      .toSet
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
    1.0 / (1.0 + Math.pow(10, (loserRating - winnerRating) / 400.0))

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

  def hopTeams(team1: Team, team2: Team): (Team, Team) = {
    val p1 = rng.shuffle(team1.members.toList).head
    val p2 = rng.shuffle(team2.members.toList).head
    (replacePlayer(team1, p1, p2), replacePlayer(team2, p2, p1))
  }

  def hopTeamsRandomly(teams: IndexedSeq[Team], hopRatioOpt: Option[Double] = None): IndexedSeq[Team] = {
    val hr = hopRatioOpt.getOrElse(hopRatio)
    val is = 1 until teams.size by 2

    val result = new scala.collection.mutable.ListBuffer[Team]()

    for (i <- is) {
      val (t1, t2) = (teams(i-1), teams(i))
      val (r1, r2) =
        if (rng.nextDouble < hr) hopTeams(t1, t2)
        else (t1, t2)
      result += r1
      result += r2
    }

    if (is.last != teams.indices.last)
      result +=  teams.last

    result.toIndexedSeq
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
                     teamsOpt: Option[IndexedSeq[Team]] = None,
                     hopTeams: IndexedSeq[Team] => IndexedSeq[Team] = hopTeamsRandomly(_, None),
                     pickGames: IndexedSeq[(Team, Double)] => Set[Game] = pickGamesFromBuckets,
                     i: Int = 0)
  : Stream[DataPoint] = {

    val ladderInput: CharacterLadder = ladderOpt.getOrElse(genLadder(ladderSize))

    val ladder: CharacterLadder =
      if (i % turnsPerWeek != 0) ladderInput
      else resetWeeklyStats(ladderInput)

    val prevTeams: IndexedSeq[Team] = teamsOpt.getOrElse {
      val ids = ladder.rows.keys.toSeq
      val result = ids.grouped(teamSize).map(p => Team(p.toSet)).toIndexedSeq
      if (result.last.members.size == teamSize) result else result.init
    }

    def withRating(teams: IndexedSeq[Team]): IndexedSeq[(Team, Double)] =
      teams.zip(teams.map(ladder.calcTeamRating))

    def sortByRating(teams: IndexedSeq[Team]): IndexedSeq[Team] =
      withRating(teams).sortBy(_._2).map(_._1)

    val teams = hopTeams(sortByRating(prevTeams))

    val matches = pickGames(withRating(teams))

    val nextLadder: CharacterLadder = matches.foldLeft(ladder) { (l, t) => play(l, t._1, t._2) }

    print(s":$i")
    (LadderUpdate(ladder, nextLadder), matches) #::
      updatesStream(Some(nextLadder), Some(teams), hopTeams, pickGames, i + 1)
  }
}
