package info.fotm.aether

import com.github.nscala_time.time.Imports
import info.fotm.domain.TeamSnapshot.SetupFilter
import info.fotm.domain._
import com.github.nscala_time.time.Imports._

import scala.collection.immutable
import scala.collection.immutable.TreeMap

/*
 Update happens once every 2 minutes.
 Reads happen more often.

 Read queries:
√ a) get team snapshots in time interval (Playing Now)
    Interval => Seq[TeamSnapshot]

  b) get setups breakdown in time interval (Leaderboard setups)
    Interval => Seq[FotmSetup]

  c) get team snapshots in time interval for particular setup (Leaderboard filter)
    (Interval, FotmSetup) => Seq[TeamSnapshot]

√ d) get history for a team
    (Interval, Team) => Seq[TeamSnapshot]

√ e) get char snapshots in time interval
    Interval => Seq[CharacterSnapshot]

√ f) get history for character
    (Interval, Team) => Seq[CharacterSnapshot]
 */
final case class StorageAxis(
                              teamHistories: Map[Team, TreeMap[DateTime, TeamSnapshot]] = Map.empty.withDefaultValue(TreeMap.empty),
                              charHistories: Map[CharacterId, TreeMap[DateTime, CharacterSnapshot]] = Map.empty.withDefaultValue(TreeMap.empty),
                              teamsSeen: TreeMap[DateTime, Set[Team]] = TreeMap.empty,
                              charsSeen: TreeMap[DateTime, Set[CharacterId]] = TreeMap.empty) {

  import StorageAxis.inInterval

  def update(teamUpdates: Seq[TeamUpdate], charUpdates: Set[CharacterSnapshot], time: DateTime = DateTime.now): StorageAxis = {

    val charsUpdated = charUpdates.map(_.id)

    val nextTeamsSeen = teamsSeen + (time -> teamUpdates.map(_.view.teamId).toSet)
    val nextCharsSeen = charsSeen + (time -> charsUpdated)

    val nextTeamHistories =
      teamUpdates.foldLeft(teamHistories) { (currentHistories, tu: TeamUpdate) =>

        val expandedHistory: TreeMap[DateTime, TeamSnapshot] =
          teamHistories.get(tu.view.teamId).fold(TreeMap(time -> TeamSnapshot.fromUpdate(tu))) { th =>
            val previousStats = th.last._2.stats

            val currentSnapshot = TeamSnapshot(
              team = tu.view.teamId,
              view = tu.view,
              stats = if (tu.won) previousStats.win else previousStats.loss)

            th + (time -> currentSnapshot)
          }

        currentHistories.updated(tu.view.teamId, expandedHistory)
      }

    val nextCharHistories =
      charUpdates.foldLeft(charHistories) { (currentHistories, next: CharacterSnapshot) =>
        val expandedHistory = charHistories.get(next.id).fold(TreeMap(time -> next)) { ch =>
          ch + (time -> next)
        }

        currentHistories.updated(next.id, expandedHistory)
      }

    StorageAxis(nextTeamHistories, nextCharHistories, nextTeamsSeen, nextCharsSeen)
  }

  def all(interval: Interval, cutoff: Int = 0, filter: SetupFilter = Seq.empty): (Seq[FotmSetup], Seq[TeamSnapshot], Seq[CharacterSnapshot]) = {

    val allTeams = inInterval(teamsSeen, interval).flatten.toSet

    val aboveCutoff =
      for {
        team <- allTeams
        teamHistory = teamHistories(team)
        if teamHistory.size > cutoff
      } yield teamHistory

    val allSnapshots: Set[TeamSnapshot] = aboveCutoff.flatMap(_.values)

    val total: Int = allSnapshots.size

    // setups
    val setupsPopularity: Map[Seq[Int], Int] =
      allSnapshots
      .groupBy(_.view.sortedSnapshots.map(_.view.specId))
      .mapValues(_.count(_.matchesFilter(filter)))
      .filter(_._2 != 0)

    val setups = for ((setup, size) <- setupsPopularity) yield FotmSetup(setup, size / total.toDouble)

    val allChars: Map[CharacterId, CharacterSnapshot] =
      chars(interval).map(c => (c.id, c))(scala.collection.breakOut)

    // teams
    val lastSnapshots: Seq[TeamSnapshot] = aboveCutoff.map(_.values.last).filter(_.matchesFilter(filter)).toSeq

    // filter out chars seen in teams that are sent back
    val charsInTeams = lastSnapshots.flatMap(_.team.members)
    val charsNotInTeams = (allChars -- charsInTeams).values.toSeq.sortBy(- _.stats.rating)

    (setups.toSeq.sortBy(- _.ratio), lastSnapshots.toSeq.sortBy(- _.rating), charsNotInTeams)
  }

  // a
  def teams(interval: Interval): Set[TeamSnapshot] =
    inInterval(teamsSeen, interval)
      .flatten
      .map(t => inInterval(teamHistories(t), interval).last)
      .toSet

  // b
  def setups(interval: Interval, cutoff: Int = 0): Seq[FotmSetup] = {
    val teamIds: Set[Team] = inInterval(teamsSeen, interval).flatten.toSet
    val snapshots: Set[TeamSnapshot] =
      teamIds.filter(teamHistories(_).size > cutoff).flatMap(teamHistories(_).values)
    val setupPop: Map[Seq[Int], Int] = snapshots.groupBy(_.view.sortedSnapshots.map(_.view.specId)).mapValues(_.size)
    val total = setupPop.values.sum.toDouble

    val result = for ((setup, size) <- setupPop) yield FotmSetup(setup, size / total)
    result.toSeq
  }

  // c
  def setupTeams(interval: Interval, setup: FotmSetup): Seq[TeamSnapshot] = {
    // TODO: implement this and tests
    ???
  }

  // d
  def teamHistory(interval: Interval, team: Team): Seq[TeamSnapshot] =
    inInterval(teamHistories(team), interval).toSeq

  // e
  def chars(interval: Interval): Seq[CharacterSnapshot] =
    inInterval(charsSeen, interval)
      .flatten
      .map(c => inInterval(charHistories(c), interval).last)
      .toSeq

  // f
  def charHistory(interval: Interval, char: CharacterId): Seq[CharacterSnapshot] =
    inInterval(charHistories(char), interval).toSeq
}

object StorageAxis {
  def inInterval[T](history: TreeMap[DateTime, T], interval: Interval): Iterable[T] =
    history.from(interval.start).to(interval.end).values
}
