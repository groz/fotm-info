import info.fotm.domain.{LadderUpdate, TeamSnapshot, CharacterLadder, Team}
import scala.collection.immutable.IndexedSeq
import org.scalatest._

class ClusteringEvaluatorDataSpec extends FlatSpec with Matchers with ClusteringEvaluatorSpecBase {
  import gen._

  "Team rating" should "be mean of players' ratings" in {
    TeamSnapshot.fromLadder(team1580, ladder).rating should be(1580)
  }

  it should "be mean of players' ratings 2" in {
    TeamSnapshot.fromLadder(team1500, ladder).rating should be(1500)
  }

  it should "be mean of players' ratings 3" in {
    val team = Team(Seq(player1500, player1580).map(_.id).toSet)
    TeamSnapshot.fromLadder(team, ladder).rating should be (1540)
  }

  //def calcRatingChange(winnerRating: Double, loserRating: Double): Int = {
  "calcRatingChange" should "output 16 for equal teams" in {
    calcRatingChange(1600, 1600) should be (16)
  }

  it should "output 20 for 1500 and 1580" in {
    calcRatingChange(1500, 1580) should be (20)
  }

  it should "output 12 for 1580 and 1500" in {
    calcRatingChange(1580, 1500) should be (12)
  }

  //def calcRating(charInfo: CharacterInfo, team: Team, won: Boolean): Int = {
  "calcRating" should "increase rating by 16 if player wins over equal team" in {
    calcRating(player1500.id, team1500, won = true)(ladder) should be(1516)
  }

  it should  "decrease rating by 16 if player loses to equal team" in {
    calcRating(player1500.id, team1500, won = false)(ladder) should be (1484)
  }

  it should "increase rating by 20 if 1500 player wins over 1580 team" in {
    calcRating(player1500.id, team1580, won = true)(ladder) should be(1520)
  }

  it should "decrease rating by 12 if 1500 player loses to 1580 team" in {
    calcRating(player1500.id, team1580, won = false)(ladder) should be(1488)
  }

  it should "increase rating by 12 if 1580 player wins over 1500 team" in {
    calcRating(player1580.id, team1500, won = true)(ladder) should be(1592)
  }

  it should "decrease rating by 20 if 1580 player loses to 1500 team" in {
    calcRating(player1580.id, team1500, won = false)(ladder) should be(1560)
  }

  "play" should "change ratings for all players accordingly" in {
    val nextLadder: CharacterLadder = play(ladder, team1580, team1500)
    team1500.members.map(nextLadder).foreach { _.rating should be(1488) } // zieg
    team1580.members.map(nextLadder).foreach { _.rating should be(1592) }
  }

  it should "adjust stats for all players involved" in {
    val nextLadder: CharacterLadder = play(ladder, team1580, team1500)
    team1500.members.foreach { c =>
      val (prev, next) = (ladder(c), nextLadder(c))
      next.season.total should be(prev.season.total + 1)
      next.season.wins should be(prev.season.wins)
      next.season.losses should be(prev.season.losses + 1)

      next.weekly.total should be(prev.weekly.total + 1)
      next.weekly.wins should be(prev.weekly.wins)
      next.weekly.losses should be(prev.weekly.losses + 1)
    }

    team1580.members.foreach { c =>
      val (prev, next) = (ladder(c), nextLadder(c))
      next.season.total should be(prev.season.total + 1)
      next.season.wins should be(prev.season.wins + 1)
      next.season.losses should be(prev.season.losses)

      next.weekly.total should be(prev.weekly.total + 1)
      next.weekly.wins should be(prev.weekly.wins + 1)
      next.weekly.losses should be(prev.weekly.losses)
    }
  }

  "prepare data" should "return correct first ladder" in {
    val firstMatches = (team1580, team1500)
    val data: Stream[(LadderUpdate, Set[(Team, Team)])] =
      updatesStream(
        Some(ladder),
        Some(IndexedSeq(team1500, team1580)),
        identity,
        (_, _) => Set(firstMatches))

    val (ladderUpdate, matchesPlayed) = data.head
    ladderUpdate.previous.rows should contain theSameElementsAs ladder.rows

    team1500.members.map(ladderUpdate.current).foreach { _.rating should be(1488) }
    team1580.members.map(ladderUpdate.current).foreach { _.rating should be(1592) }
    matchesPlayed should contain theSameElementsAs Seq(firstMatches)
  }

  it should "make correct transition to second ladder" in {
    val matches = (team1580, team1500)
    val data: Stream[(LadderUpdate, Set[(Team, Team)])] =
      updatesStream(
        Some(ladder),
        Some(IndexedSeq(team1500, team1580)),
        identity,
        (_, _) => Set(matches))

    val (previousUpdate, firstMatchesPlayed) = data.head
    val (currentUpdate, secondMatchesPlayed) = data.tail.head

    currentUpdate.previous.rows should contain theSameElementsAs previousUpdate.current.rows

    team1500.members.map(currentUpdate.current).foreach { _.rating should be(1477) }
    team1580.members.map(currentUpdate.current).foreach { _.rating should be(1603) }
    secondMatchesPlayed should contain theSameElementsAs Seq(matches)
  }

  "replace player" should "remove one player and add another" in {
    val rt = replacePlayer(team1500, player1500.id, player1580.id)
    rt.members should contain (player1580.id)
    rt.members shouldNot contain (player1500.id)
  }

  it should "work when the players are the same" in {
    val rt = replacePlayer(team1500, player1500.id, player1500.id)
    rt.members.size should be(3)
    rt.members should contain (player1500.id)
  }

  it should "work when the players are in the same team" in {
    val rt = replacePlayer(team1500, team1500.members.head, team1500.members.last)
    rt.members.size should be(3)
    team1500.members.foreach { rt.members should contain (_) }
  }

  "hop teams" should "exhange players between 1500 and 1580 teams" in {
    val (t1, t2) = hopTeams(team1500, team1580)
    t1.members.intersect(team1580.members).size should be(1)
    t2.members.intersect(team1500.members).size should be(1)
  }

  it should "handle exchange within the same team" in {
    val (t1, t2) = hopTeams(team1500, team1500)
    t1.members.size should be(3)
    t2 should be(t1)
  }

  "hopTeamsRandomly" should "preserve the number of teams" in {
    hopTeamsRandomly(IndexedSeq(team1500, team1580)).size should be(2)
  }

  it should "correctly swap players between teams" in {
    // TODO: implement this
  }

  "resetWeeklyStats" should "reset weekly stats for all players in ladder" in {
    val nextLadder: CharacterLadder = play(ladder, team1580, team1500)

    val resetLadder = resetWeeklyStats(nextLadder)

    team1580.members.map(resetLadder).foreach { c =>
      c.weekly.total should be(0)
      c.weekly.wins should be(0)
      c.weekly.losses should be(0)
    }

    team1500.members.map(resetLadder).foreach { c =>
      c.weekly.total should be(0)
      c.weekly.wins should be(0)
      c.weekly.losses should be(0)
    }
  }

}
