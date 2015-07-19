package info.fotm.clustering

final case class EvaluatorSettings(
    matchesPerTurn: Int = 30,
    ladderSize: Int = 5000,
    teamSize: Int = 3,
    hopRatio: Double = 0.05,
    turnsPerWeek: Int = 1000) {

  val startTurn = turnsPerWeek * 7/3            // mid third week
  val endTurn = startTurn + 1 * turnsPerWeek    // sim for 1 week
}

case object Defaults {
  lazy val settings = List(2, 3, 5, 10).map(size => (size, EvaluatorSettings(teamSize = size))).toMap
  lazy val generators = settings.map(kv => kv._1 -> new ClusteringEvaluatorData(kv._2))
}

