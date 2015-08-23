import info.fotm.clustering.ClusteringEvaluatorData
import info.fotm.domain.TeamSnapshot.SetupFilter
import info.fotm.domain.{CharacterSnapshot, TeamSnapshot}
import org.scalatest.{FlatSpec, Matchers}

class DomainSpec extends FlatSpec with Matchers {

  val data = new ClusteringEvaluatorData()

  def createChar(classId: Int, specId: Int): CharacterSnapshot = {
    val ss = data.genPlayer()
    ss.copy(
      id = ss.id.copy(classId = classId),
      view = ss.view.copy(specId = specId)
    )
  }

  "TeamSnapshot.matchesFilter" should "match empty filters" in {
    val chars: Set[CharacterSnapshot] = Set( createChar(10, 1), createChar(10, 2) )
    val filter: SetupFilter = Seq.empty
    TeamSnapshot.matchesFilter(chars, filter) should be(true)
  }

  "TeamSnapshot.matchesFilter" should "match [(10, 1), (10, 2)] vs [(10, 1)]" in {
    val chars: Set[CharacterSnapshot] = Set( createChar(10, 1), createChar(10, 2) )
    val filter: SetupFilter = Seq( (10, Some(1)) )
    TeamSnapshot.matchesFilter(chars, filter) should be(true)
  }

  "TeamSnapshot.matchesFilter" should "match [(10, 1), (10, 1), (10, 2)] vs [(10, 2), (10, 1)]" in {
    val chars: Set[CharacterSnapshot] = Set( createChar(10, 1), createChar(10, 1), createChar(10, 2) )
    val filter: SetupFilter = Seq( (10, Some(2)), (10, Some(1)) )
    TeamSnapshot.matchesFilter(chars, filter) should be(true)
  }

  "TeamSnapshot.matchesFilter" should "not match [(10, 1), (10, 2), (10, 2)] vs [(10, 1), (10, 1)]" in {
    val chars: Set[CharacterSnapshot] = Set( createChar(10, 1), createChar(10, 2), createChar(10, 2) )
    val filter: SetupFilter = Seq( (10, Some(1)), (10, Some(1)) )
    TeamSnapshot.matchesFilter(chars, filter) should be(false)
  }

  "TeamSnapshot.matchesFilter" should "not match [(10, 1), (10, 2)] vs [(10, 1), (10, 3)]" in {
    val chars: Set[CharacterSnapshot] = Set( createChar(10, 1), createChar(10, 2) )
    val filter: SetupFilter = Seq( (10, Some(1)), (10, Some(3)) )
    TeamSnapshot.matchesFilter(chars, filter) should be(false)
  }

  "TeamSnapshot.matchesFilter" should "match [(10, 1), (10, 2)] vs [(10, 1), (10, *)]" in {
    val chars: Set[CharacterSnapshot] = Set( createChar(10, 1), createChar(10, 2) )
    val filter: SetupFilter = Seq( (10, Some(1)), (10, None) )
    TeamSnapshot.matchesFilter(chars, filter) should be(true)
  }

  "TeamSnapshot.matchesFilter" should "not match [(9, 1), (10, 2)] vs [(10, *), (10, *)]" in {
    val chars: Set[CharacterSnapshot] = Set( createChar(9, 1), createChar(10, 2) )
    val filter: SetupFilter = Seq( (10, None), (10, None) )
    TeamSnapshot.matchesFilter(chars, filter) should be(false)
  }


  "TeamSnapshot.matchesFilter" should "match [(9, 11), (10, 1), (10, 2)] vs [(9, *), (10, 1), (10, *)]" in {
    val chars: Set[CharacterSnapshot] = Set( createChar(9, 11), createChar(10, 1), createChar(10, 2) )
    val filter: SetupFilter = Seq( (9, None), (10, Some(1)), (10, None) )
    TeamSnapshot.matchesFilter(chars, filter) should be(true)
  }

  "TeamSnapshot.matchesFilter" should "match [(9, 11), (10, 1)] vs [(9, *), (10, *)]" in {
    val chars: Set[CharacterSnapshot] = Set( createChar(9, 11), createChar(10, 1) )
    val filter: SetupFilter = Seq( (9, None), (10, None) )
    TeamSnapshot.matchesFilter(chars, filter) should be(true)
  }

  "TeamSnapshot.matchesFilter" should "not match [(9, 11), (10, 1), (10, 2)] vs [(9, *), (9, *), (10, *)]" in {
    val chars: Set[CharacterSnapshot] = Set( createChar(9, 11), createChar(10, 1), createChar(10, 2) )
    val filter: SetupFilter = Seq( (9, None), (9, None), (10, None) )
    TeamSnapshot.matchesFilter(chars, filter) should be(false)
  }

}
