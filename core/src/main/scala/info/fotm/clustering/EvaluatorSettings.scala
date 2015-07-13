package info.fotm.clustering

final case class EvaluatorSettings(
    matchesPerTurn: Int = 20,
    ladderSize: Int = 1000,
    teamSize: Int = 5,
    hopRatio: Double = 0.05)

case object Defaults {
  lazy val settings = List(2, 3, 5, 10).map(size => (size, EvaluatorSettings(teamSize = size))).toMap
  lazy val generators = settings.map(kv => kv._1 -> new ClusteringEvaluatorData(kv._2))
}

