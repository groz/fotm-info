package info.fotm.domain

final case class Stats(wins: Int, losses: Int) {
  val total = wins + losses
  def win = Stats(wins + 1, losses)
  def loss = Stats(wins, losses + 1)
}

object Stats {
  val empty = Stats(0, 0)
}
