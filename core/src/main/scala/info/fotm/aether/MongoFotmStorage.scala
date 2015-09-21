package info.fotm.aether

import com.github.nscala_time.time.Imports._
import info.fotm.aether.FotmStorage._
import info.fotm.domain.TeamSnapshot.SetupFilter
import info.fotm.domain._
import play.api.libs.json.{OFormat, Json}
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

import scala.collection.immutable.TreeMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/*
Requires following indices created:

db.v4_teams.createIndex( { axis: 1, time: -1 } )
db.v4_chars.createIndex( { axis: 1, time: -1 } )
db.v4_teams_latest.createIndex( { axis: 1, time: -1 } )

TODO:
  additional optimizations can be made if we separate queries
  with setupFilter and without.
  that will allow to add pagination to db requests because we
  will know how many entries we need
 */

final case class TeamSnapshotDocument(axis: Axis, time: DateTime, value: TeamSnapshot)

final case class CharSnapshotDocument(axis: Axis, time: DateTime, value: CharacterSnapshot)

object MongoFotmStorage {
  lazy val driver = new MongoDriver(Some(AetherConfig.config))

  lazy val connection = MongoConnection.parseURI(AetherConfig.dbPath).map(MongoFotmStorage.driver.connection).get

  lazy val db = connection("fotmdb")
}

class MongoFotmStorage extends FotmStorage {

  import JsonFormatters._
  import MongoFotmStorage._

  val teamsCollection = db.collection[JSONCollection]("v4_teams")
  val charsCollection = db.collection[JSONCollection]("v4_chars")

  val latestTeamsCollection = db.collection[JSONCollection]("v4_teams_latest")

  implicit val teamOFmt: OFormat[Team] = oformat[Team](teamFmt)
  implicit val charOFmt: OFormat[CharacterId] = oformat[CharacterId](charIdFmt)

  implicit val teamSnapshotDocOFmt = oformat[TeamSnapshotDocument](Json.format)
  implicit val charSnapshotDocOFmt = oformat[CharSnapshotDocument](Json.format)

  def getLatestTeams(axis: Axis, teamIds: Seq[Team]): Future[Map[Team, Option[TeamSnapshotDocument]]] =
    Future.sequence(teamIds.map { teamId =>
      val doc = latestTeamsCollection
        .find(Json.obj(
          "axis" -> Json.toJson(axis),
          "value.team" -> teamOFmt.writes(teamId)
        ))
        .cursor[TeamSnapshotDocument]()
        .headOption
      doc.map(d => (teamId, d))
    }).map(_.toMap)

  override def update(updates: Updates): Unit = {
    val time = DateTime.now
    val axis = updates.axis

    val teamIds = updates.teamUpdates.map(_.view.teamId)
    val charIds = updates.charUpdates.map(_.id)

    val latestTeamsFuture: Future[Map[Team, Option[TeamSnapshotDocument]]] = getLatestTeams(axis, teamIds)

    val teamDocumentUpdates: Future[Seq[TeamSnapshotDocument]] = latestTeamsFuture.map { latestTeams =>
      updates.teamUpdates.map { teamUpdate =>
        latestTeams(teamUpdate.view.teamId).map { lastSnapshot =>
          val newStats = if (teamUpdate.won) lastSnapshot.value.stats.win else lastSnapshot.value.stats.loss
          TeamSnapshotDocument(axis, time, lastSnapshot.value.copy(stats = newStats))
        }.getOrElse(TeamSnapshotDocument(updates.axis, time, TeamSnapshot.fromUpdate(teamUpdate)))
      }
    }

    // double writes.
    // 1) update all
    // 2) update/insert latest
    teamDocumentUpdates.foreach { lst =>
      val docs = lst.map(teamSnapshotDocOFmt.writes).toStream
      teamsCollection.bulkInsert(docs, ordered = false)
    }

    for {
      latestTeams <- latestTeamsFuture
      teamDocuments <- teamDocumentUpdates
    } {
      for ((team, snapshotOption) <- latestTeams) {
        val teamSnapshotDocument: TeamSnapshotDocument = teamDocuments.find(_.value.team == team).get

        snapshotOption.fold {
          latestTeamsCollection.insert(teamSnapshotDocument)
        } { snapshot =>
          latestTeamsCollection.update(
            Json.obj("axis" -> axis, "value.team" -> teamOFmt.writes(team)),
            Json.obj("$set" ->
              Json.obj("time" -> Json.toJson(time), "value" -> teamSsFmt.writes(teamSnapshotDocument.value))
            )
          )
        }
      }
    }

    // double writes
    val charSnapshots = updates.charUpdates.map(_.current)
    val charDocs = charSnapshots.map(cs => CharSnapshotDocument(axis, time, cs))
    val cds = charDocs.map(charSnapshotDocOFmt.writes).toStream
    charsCollection.bulkInsert(cds, ordered = false)
  }

  override def queryTeamHistory(axis: Axis, team: Team): Future[QueryTeamHistoryResponse] = {
    val teamDocsFuture: Future[List[TeamSnapshotDocument]] =
      teamsCollection
        .find(Json.obj(
          "axis" -> axis,
          "value.team" -> teamOFmt.writes(team))
        )
        .cursor[TeamSnapshotDocument]()
        .collect[List]()

    teamDocsFuture.map { (teamDocs: List[TeamSnapshotDocument]) =>
      val history = teamDocs.map(td => (td.time, td.value))
      val historyTree = TreeMap(history: _*)
      QueryTeamHistoryResponse(axis, team, historyTree)
    }
  }

  override def queryCharHistory(axis: Axis, charId: CharacterId): Future[QueryCharHistoryResponse] = {

    val lastSnapshotOptionFuture: Future[Option[CharSnapshotDocument]] =
      charsCollection
        .find(Json.obj(
          "axis" -> Json.toJson(axis),
          "value.id" -> charOFmt.writes(charId))
        )
        .cursor[CharSnapshotDocument]()
        .headOption

    val teamDocsFuture: Future[List[TeamSnapshotDocument]] =
      teamsCollection
        .find(Json.obj(
          "axis" -> Json.toJson(axis),
          "value.team.members" -> charOFmt.writes(charId))
        )
        .cursor[TeamSnapshotDocument]()
        .collect[List]()

    for {
      lastSnapshotOption <- lastSnapshotOptionFuture
      teamDocs <- teamDocsFuture
    } yield {
      val history = teamDocs.map(td => (td.time, td.value))
      val historyTree = TreeMap(history: _*)
      QueryCharHistoryResponse(axis, charId, lastSnapshotOption.map(_.value), historyTree)
    }
  }

  def inInterval[D](coll: JSONCollection, axis: Axis, interval: Interval)(implicit fmt: OFormat[D]) =
    coll
      .find(Json.obj(
        "axis" -> Json.toJson(axis),
        "time" -> Json.obj(
          "$gte" -> Json.toJson(interval.start),
          "$lte" -> Json.toJson(interval.end)
        )
      ))
      .cursor[D]()
      .collect[Set]()

  def calcFotmSetups(teamSnapshotDocs: Set[TeamSnapshotDocument], filter: SetupFilter, cutoff: Int)
    : (Seq[FotmSetup], Seq[TeamSnapshot]) = {

    val teamMap: Map[Team, Set[TeamSnapshotDocument]] =
      teamSnapshotDocs
        .groupBy(_.value.view.teamId)
        .filter(_._2.size > cutoff)

    val allSnapshots: Set[TeamSnapshot] =
      teamMap.values.flatMap(tsd => tsd.map(_.value)).toSet

    val total: Int = allSnapshots.size

    // setups
    val setupsPopularity: Map[Seq[Int], Int] =
      allSnapshots
        .groupBy(_.view.sortedSnapshots.map(_.view.specId))
        .mapValues(_.count(_.matchesFilter(filter)))
        .filter(_._2 != 0)

    val setups = for ((setup, size) <- setupsPopularity) yield FotmSetup(setup, size / total.toDouble)

    val resultTeams = teamMap.values
      .map(_.maxBy(td => td.time).value)
      .filter(_.matchesFilter(filter))
      .toSeq

    (setups.toSeq.sortBy(-_.ratio), resultTeams.sortBy(-_.rating))
  }

  def queryAllNoChars(axis: Axis, interval: Interval, filter: SetupFilter): Future[QueryAllResponse] = {
    val teamsInIntervalFuture = inInterval[TeamSnapshotDocument](teamsCollection, axis, interval)

    teamsInIntervalFuture.onFailure {
      case ex => println(s"Query teams failed: $ex")
    }

    for {
      teamsInInterval: Set[TeamSnapshotDocument] <- teamsInIntervalFuture
    } yield {
      val (setups, teams) = calcFotmSetups(teamsInInterval, filter, cutoff = 1)
      QueryAllResponse(axis, setups, teams, Seq.empty)
    }
  }

  def queryAllWithChars(axis: Axis, interval: Interval, filter: SetupFilter): Future[QueryAllResponse] = {
    val teamsInIntervalFuture = inInterval[TeamSnapshotDocument](teamsCollection, axis, interval)

    teamsInIntervalFuture.onFailure {
      case ex => println(s"Query teams failed: $ex")
    }

    val charsInIntervalFuture = inInterval[CharSnapshotDocument](charsCollection, axis, interval)

    charsInIntervalFuture.onFailure {
      case ex => println(s"Query chars failed: $ex")
    }

    for {
      teamsInInterval: Set[TeamSnapshotDocument] <- teamsInIntervalFuture
      charsInInterval: Set[CharSnapshotDocument] <- charsInIntervalFuture
    } yield {
      val (setups, teams) = calcFotmSetups(teamsInInterval, filter, cutoff = 1)

      val allChars = charsInInterval.groupBy(cd => cd.value.id).mapValues(_.maxBy(_.time).value)
      val charsInTeams: Set[CharacterId] = teamsInInterval.flatMap(_.value.team.members)
      val charsNotInTeams: Map[CharacterId, CharacterSnapshot] = allChars -- charsInTeams

      val filteredChars =
        for{
          (id, snapshot) <- charsNotInTeams
          if snapshot.matchesFilter(filter)
        }
        yield snapshot

      QueryAllResponse(axis, setups, teams, filteredChars.toSeq.sortBy(-_.stats.rating))
    }
  }

  override def queryAll(axis: Axis, interval: Interval, filter: SetupFilter, showChars: Boolean): Future[QueryAllResponse] = {
    if (showChars)
      queryAllWithChars(axis, interval, filter)
    else
      queryAllNoChars(axis, interval, filter)
  }
}
