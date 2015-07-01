package info.fotm.clustering.RMClustering

class DijkstraAlg(graph: Graph[Int])
{
  /**
   * Returns shortest (using mod distance) paths from startVertices to all vertices
   * @param startVertices
   * @return
   */
  def findShortestPathsFrom(startVertices: Set[Int]): Set[GraphPath] =
  {
    startVertices.flatMap(findShortestPathsFrom)
  }

  /**
   * Returns shortest paths from startVertex to all other vertices
   * @param startVertex
   * @return
   */
  private def findShortestPathsFrom(startVertex: Int): Set[GraphPath] =
  {
    val labels =
      new Label(startVertex, 0, false) ::
        (0 until graph.vertexCount).filter(j => j != startVertex).map(j => Label(j)).toList

    val finalLabels = visitNext(labels)
    generatePaths(finalLabels)
  }

  private def visitNext(labels: List[Label]): List[Label] =
  {
    val (visited, unvisited) = labels.partition(x => x.visited)

    val minLabel = unvisited.minBy(x => x.distance)

    val updatedLabels =
      for
      {
        label <- unvisited
        if label != minLabel
        newdist = graph.distance(minLabel.vertex, label.vertex).max(minLabel.distance)
        //newdist = graph.distance(minLabel.vertex, label.vertex) + (minLabel.distance) // classic Dijkstra
        newLabel =
        if (newdist < label.distance)
          Label(label.vertex, newdist, prevVertex = minLabel.vertex)
        else
          label
      } yield newLabel

    val newLabels = minLabel.makeVisited :: visited ++ updatedLabels

    if (updatedLabels.length > 1)
      visitNext(newLabels)
    else
      newLabels
  }


  /**
   *
   * @param labels
   * @return
   */
  private def generatePaths(labels: List[Label]): Set[GraphPath] =
  {
    val sortedLabels = labels.sortBy(x => x.vertex)

    def makePath(label: Label): GraphPath =
    {
      val path: Vector[Int] = nextVertex(List(label.prevVertex, label.vertex)).toVector
      new GraphPath(label.distance, path)
    }

    def nextVertex(path: List[Int]): List[Int] =
    {
      val label = sortedLabels(path.head)
      if (label.prevVertex == -1)
        path
      else
        nextVertex(label.prevVertex :: path)
    }

    labels.filter(x => x.prevVertex != -1).map(makePath).toSet
  }

  case class Label(vertex: Int, distance: Double = Double.MaxValue, visited: Boolean = false, prevVertex: Int = -1)
  {
    def makeVisited = Label(vertex, distance, true, prevVertex)
  }

}
