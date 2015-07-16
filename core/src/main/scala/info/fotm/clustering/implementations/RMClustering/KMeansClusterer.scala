package info.fotm.clustering.implementations.RMClustering

import info.fotm.clustering.Clusterer
import info.fotm.clustering.Clusterer.{V, Cluster}

//import scala.util.Random
// https://en.wikipedia.org/wiki/K-means_clustering
class KMeansClusterer extends Clusterer {

  val rng = new scala.util.Random(100)

  def clusterize(input: Cluster, clustersCount: Int): Set[Cluster] =
  {
    val means = initialize_plusplus(input, clustersCount)
    process(Set(input),means)
  }

  def clusterize(input: Cluster, clustersCount: Int, initCenters: Seq[V]): Set[Cluster] =
  {
    require(clustersCount == initCenters.length)
    process(Set(input), initCenters)
  }

  def process(clusters: Set[Cluster], means: Seq[V]): Set[Cluster] =
  {
    val newClusters = assignment(clusters,means)

    if (newClusters!=clusters)
    {
      val newMeans = update(newClusters)
      process(newClusters,newMeans)
    }
    else
      newClusters
  }

  /** *
    * Produces initial means
    * @param input
    * @param clustersCount
    * @return
    */
  def initialize(input: Cluster, clustersCount: Int): Seq[V] =
  {
    rng.shuffle(input).take(clustersCount)
  }

  //=====================Plus Plus section=======================
  def initialize_plusplus(input: Cluster, clustersCount: Int): Seq[V] =
  {
    val firstCenterIndex = rng.nextInt(input.length)
    val centers = input(firstCenterIndex) :: Nil
    findCenters(input.filterNot(x => x == centers(0)), centers, clustersCount)
  }

  /**
   * Adds one center to centers from input using a weighted probability distribution
   * @param input
   * @param centers
   * @param centersCount
   * @return
   */
  def findCenters(input: Cluster, centers: List[V], centersCount: Int): Seq[V] =
  {
    if (centers.length < centersCount && input.length > 0)
    {
      //? Странно, что fold тут требует, чтобы startValue был supertype коллекции input
      //? https://coderwall.com/p/4l73-a/scala-fold-foldleft-and-foldright
      // inputDistancesToCenters(i) is a probability of i-th point from input to be a center
      val inputDistancesToCenters = input.map(x => centers.foldLeft(Double.MaxValue){
                                      (acc,c) => acc.min(distance(c,x))
                                    })
      val newCenter = getRandomValue(input, inputDistancesToCenters)
      findCenters(input.filterNot(x => x == newCenter), newCenter :: centers, centersCount)
    }
    else
      centers
  }

  def getRandomValue(input: Cluster, probabilities: Seq[Double]): V =
  {
    // overflow protection coefficient
    val max = probabilities.max

    // make distribution function from probabilities: (5,2,4,5) -> (5,7,11,16)/max = (5,7,11,16)/5
    val distribution = probabilities.foldLeft(List[Double](0)){
      (list,x) => list.head + x / max :: list
    }.init.reverse

    val randomInterval = distribution.last
    val rand = rng.nextDouble() * randomInterval

    // getting index corresponding to rand and return element from input with this index
    input(distribution.span(x => x < rand)._1.length)
  }
  //=====================/Plus Plus section=======================


  def assignment(input: Set[Cluster], means: Seq[V]): Set[Cluster] =
  {
    input.toSeq.flatten.groupBy(v=>means.minBy(distance(_,v))).mapValues(s=>s.toVector).values.toSet
  }

  def update(clusters: Set[Cluster]): Seq[V] =
  {
    clusters.map(c=> div(c.reduce(sumOf),c.length)).toSeq
  }

  def distance(v1: V, v2: V): Double = v1.distTo1(v2) //v1.zip(v2).map(x=>Math.pow(x._1-x._2,2)).sum

  def sumOf(v1: V, v2: V): V = v1 + v2 //v1.zip(v2).map({case(x1,x2)=>x1+x2})

  def div(v: V, byValue: Double): V = v/ byValue // v.map(x=>x/byValue)
}
