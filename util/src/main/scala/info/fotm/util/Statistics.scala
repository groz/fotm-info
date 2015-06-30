package info.fotm.util

object Statistics {

  // https://en.wikipedia.org/wiki/F1_score
  def fScore(beta: Double)(tp: Int, fp: Int, fn: Int) = {
    val betaSquared = beta * beta
    val nom = (1 + betaSquared) * tp
    nom / (nom + betaSquared * fn + fp)
  }

  def f1Score = fScore(1.0) _
}
