package info.fotm.domain

case class TeamStats(rating: Int, wins: Int, losses: Int) {
  val total = wins + losses
  def win = copy(wins = wins + 1)
  def lose = copy(losses = losses + 1)
}

case object TeamStats {
  def empty(rating: Int) = TeamStats(rating, 0, 0)
}