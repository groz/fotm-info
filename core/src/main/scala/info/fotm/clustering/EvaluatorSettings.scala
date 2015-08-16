package info.fotm.clustering

final case class EvaluatorSettings(
    matchesPerTurn: Int = 20,
    ladderSize: Int = 5000,
    teamSize: Int = 3,
    hopRatio: Double = 0.05,
    turnsPerWeek: Int = 500) {

  val startTurn = turnsPerWeek * 10/3
  val endTurn = startTurn + 2 * turnsPerWeek
}

case object Defaults {
  lazy val settings = List(2, 3, 5, 10).map(size => (size, EvaluatorSettings(teamSize = size))).toMap
  lazy val generators = settings.map(kv => kv._1 -> new ClusteringEvaluatorData(kv._2))
}
