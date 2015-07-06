package info.fotm.domain

case class TeamStats(wins: Int, losses: Int) {
  val total = wins + losses
  def win = copy(wins = wins + 1)
  def lose = copy(losses = losses + 1)
}

case object TeamStats {
  val empty = TeamStats(0, 0)
}