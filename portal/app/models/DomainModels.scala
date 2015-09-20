package models

import info.fotm.domain.{FotmSetup, CharacterSnapshot}
import info.fotm.domain.TeamSnapshot.SetupFilter

final case class ClassModel(id: Int, name: String, specs: Int*)

object DomainModels {

  val factions = Map(
    0 -> "Alliance",
    1 -> "Horde"
  )

  val genders = Map(
    0 -> "Male",
    1 -> "Female"
  )

  val races = Map(
    1 -> "Human",
    2 -> "Orc",
    3 -> "Dwarf",
    4 -> "Night Elf",
    5 -> "Undead",
    6 -> "Tauren",
    7 -> "Gnome",
    8 -> "Troll",
    9 -> "Goblin",
    10 -> "Blood Elf",
    11 -> "Draenei",
    22 -> "Worgen",
    24 -> "Pandaren",
    25 -> "Pandaren",
    26 -> "Pandaren"
  )

  var classes = Map(
    1 -> ClassModel(1, "Warrior", 71, 72, 73),
    2 -> ClassModel(2, "Paladin", 65, 66, 70),
    3 -> ClassModel(3, "Hunter", 253, 254, 255),
    4 -> ClassModel(4, "Rogue", 259, 260, 261),
    5 -> ClassModel(5, "Priest", 256, 257, 258),
    6 -> ClassModel(6, "Death Knight", 250, 251, 252),
    7 -> ClassModel(7, "Shaman", 262, 263, 264),
    8 -> ClassModel(8, "Mage", 62, 63, 64),
    9 -> ClassModel(9, "Warlock", 265, 266, 267),
    10 -> ClassModel(10, "Monk", 268, 269, 270),
    11 -> ClassModel(11, "Druid", 102, 103, 104, 105)
  )

  var specs = Map(
    62 -> "Arcane Mage",
    63 -> "Fire Mage",
    64 -> "Frost Mage",
    65 -> "Holy Paladin",
    66 -> "Protection Paladin",
    70 -> "Retribution Paladin",
    71 -> "Arms Warrior",
    72 -> "Fury Warrior",
    73 -> "Protection Warrior",
    102 -> "Balance Druid",
    103 -> "Feral Druid",
    104 -> "Guardian Druid",
    105 -> "Restoration Druid",
    250 -> "Blood Death Knight",
    251 -> "Frost Death Knight",
    252 -> "Unholy Death Knight",
    253 -> "Beast Mastery Hunter",
    254 -> "Marksmanship Hunter",
    255 -> "Survival Hunter",
    256 -> "Discipline Priest",
    257 -> "Holy Priest",
    258 -> "Shadow Priest",
    259 -> "Assasination Rogue",
    260 -> "Combat Rogue",
    261 -> "Subtlety Rogue",
    262 -> "Elemental Shaman",
    263 -> "Enhancement Shaman",
    264 -> "Restoration Shaman",
    265 -> "Affliction Warlock",
    266 -> "Demonology Warlock",
    267 -> "Destruction Warlock",
    268 -> "Brewmaster Monk",
    269 -> "Windwalker Monk",
    270 -> "Mistweaver Monk"
  )

  val specsToClass: Map[Int, Int] = for {
    (classId, classInfo) <- classes
    specId <- classInfo.specs
  } yield (specId, classId)

  def filterToString(setupFilter: SetupFilter): String =
    setupFilter.map(kv => s"${kv._1}-${kv._2.getOrElse(0)}").mkString(",")

  def setupToFilterString(setup: FotmSetup): String =
    filterToString( setup.specIds.map{ specId => (specsToClass(specId), Some(specId)) } )

  def raceGender(char: CharacterSnapshot) = {
  }
}
