package info.fotm.clustering.implementations.RMClustering

/**
 *
 * @param graphAdjacencyMatrix It must be square matrix
 * @param vertexLabels It is supposed that keys may have values from 0 to graphAdjacencyMatrix.length
 * @tparam T
 */
class Graph[T](graphAdjacencyMatrix: Vector[Vector[Double]], vertexLabels: Map[Int, T])
{
  val matrix = graphAdjacencyMatrix
  val labels = vertexLabels

  def apply(vertex: Int) = labels(vertex)

  def distance(v1: Int, v2: Int) = matrix(v1)(v2)

  def vertexCount = matrix.length
}
