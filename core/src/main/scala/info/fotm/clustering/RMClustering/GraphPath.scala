package info.fotm.clustering.RMClustering

class GraphPath(val length: Double, gPath: Vector[Int])
{
  val path = gPath //if (gPath.head < gPath.last) gPath else gPath.reverse

  def start = path.head

  def end = path.last

  def maxRoute(graph: Graph[Int]): Double = {
    val k = (0 to this.path.length-2).maxBy(i => graph.distance(this.path(i),this.path(i+1)))
    graph.distance(this.path(k),this.path(k+1))
  }

  def canEqual(other: Any) = other.isInstanceOf[GraphPath]

  override def equals(other: Any) =
    other match
    {
      case that: GraphPath =>
        (that canEqual this) && that.length == this.length &&
          that.path == this.path

      case _ => false
    }

  override def hashCode =
    41 * (41 + length.hashCode()) + path.hashCode()

  override def toString = s"($length,${path.toString})"
}
