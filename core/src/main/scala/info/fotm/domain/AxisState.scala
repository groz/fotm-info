package info.fotm.domain

import com.github.nscala_time.time.Imports._

/*
This class is to be used with JSON serialization to save/restore axis state until moved to MongoDB.

Note:
  It can be made more compact because we only need CharacterSnapshots and cursors into them for teams.
  But this will be gzipped in the end, so it might not save any actual space.
 */
case class AxisState(
  teams: Set[TeamSnapshot],
  chars: Set[CharacterSnapshot],
  teamsSeen: Seq[(DateTime, Set[Team])],
  charsSeen: Seq[(DateTime, Set[CharacterId])]
)
