package info.fotm.clustering

import FotmClusteringEvaluator.LadderSnapshot
import scala.util.Random

case class CharacterId(uid: String)

case class CharacterInfo(
                          id: CharacterId,
                          rating: Int,
                          weeklyWins: Int,
                          weeklyLosses: Int,
                          seasonWins: Int,
                          seasonLosses: Int)

case class CharFeatures(
                         prevInfo: CharacterInfo,
                         nextInfo: CharacterInfo)

case class Team(members: Set[CharacterId]) {
  def rating(ladder: LadderSnapshot): Int = {
    val charInfos: Set[CharacterInfo] = members.map(ladder)
    val totalRating = charInfos.toSeq.map(_.rating).sum
    val result = totalRating / members.size.toDouble
    result.toInt
  }
}

object FotmClusteringEvaluator extends App {

  type LadderSnapshot = Map[CharacterId, CharacterInfo]

  val ladderSize = 10
  val teamSize = 3
  val matchesPerTurn = 2

  lazy val rng = new Random()

  def genPlayer = {
    val id = java.util.UUID.randomUUID().toString
    CharacterInfo(CharacterId(id), 1500, 0, 0, 0, 0)
  }

  def genLadder(nPlayers: Int): LadderSnapshot = (0 until nPlayers).map(i => {
    val player = genPlayer
    (player.id, player)
  }).toMap

  def prepareData() = {
    val ladder: LadderSnapshot = genLadder(ladderSize)

    val teams: Seq[Team] = {
      val ids = ladder.keys.toSeq
      val result = ids.sliding(teamSize, teamSize).map(p => Team(p.toSet)).toSeq
      if (result.last.members.size == teamSize) result else result.init
    }

    val memberships: Map[CharacterId, Team] = {
      for {
        t <- teams
        c <- t.members
      } yield (c, t)
    }.toMap

    val shuffledTeams: Seq[Team] = new Random().shuffle(memberships.values.toSeq)

    val matches: Iterator[Seq[Team]] = shuffledTeams.sliding(2, 2).take(matchesPerTurn)

    val nextLadder: LadderSnapshot = matches.foldLeft(ladder) { (l, teams) => play(l, teams.head, teams.last) }

    val teamsPlayed: List[Team] = matches.flatten.toList
    val playersPlayed: List[CharacterId] = teamsPlayed.flatMap(_.members)

    // algo input: ladder diffs for playersPlayed
    val diffs = playersPlayed.map { p => CharFeatures(ladder(p), nextLadder(p)) }

    // algo output: players grouped into teams

    // algo evaluation: match output against teamsPlayed
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

  def calcRatingChange(winnerRating: Double, loserRating: Double): Int = {
    val chance = 1.0 / (1.0 + Math.pow(10, (loserRating - winnerRating)/400.0))
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

  genLadder(ladderSize).foreach(println)
}
