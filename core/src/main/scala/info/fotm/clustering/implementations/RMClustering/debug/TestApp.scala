package info.fotm.clustering.implementations.RMClustering

import info.fotm.clustering.Clusterer.Cluster
import info.fotm.util.MathVector

/**
 * Created by Admin on 04.07.2015.
 */
object TestApp extends App
{
  import InputData._

//  val data = List(
//  Vector(
//    MathVector(0.05555555555555555,0.024601681084288528,0.5676328502415459,0.5348837209302325,0.4,0.0,0.0),
//    MathVector(0.05555555555555555,0.024810951230356666,0.5603864734299517,0.46511627906976744,0.3111111111111111,0.0,0.0),
//    MathVector(0.05555555555555555,0.024810951230356666,0.5603864734299517,0.5348837209302325,0.17777777777777778,0.0,0.0)),
//  Vector(
//    MathVector(0.0,0.0,0.572463768115942,0.5116279069767442,0.3111111111111111,0.0,0.0),
//    MathVector(0.0,1.5958199273631473E-4,0.5652173913043478,0.5116279069767442,0.4888888888888889,0.0,0.0),
//    MathVector(0.0,5.312307224866497E-5,0.5700483091787439,0.46511627906976744,0.24444444444444444,0.0,0.0)
//  ))
//
//
//  val dataR = List(
//    Vector(
//      MathVector(0.0,5.312307224866497E-5,0.5700483091787439,0.46511627906976744,0.24444444444444444,0.0,0.0),
//      MathVector(0.05555555555555555,0.024810951230356666,0.5603864734299517,0.46511627906976744,0.3111111111111111,0.0,0.0),
//      MathVector(0.05555555555555555,0.024810951230356666,0.5603864734299517,0.5348837209302325,0.17777777777777778,0.0,0.0)),
//    Vector(
//      MathVector(0.0,0.0,0.572463768115942,0.5116279069767442,0.3111111111111111,0.0,0.0),
//      MathVector(0.0,1.5958199273631473E-4,0.5652173913043478,0.5116279069767442,0.4888888888888889,0.0,0.0),
//      MathVector(0.05555555555555555,0.024601681084288528,0.5676328502415459,0.5348837209302325,0.4,0.0,0.0)
//    ))

//  val max = ClusterRoutines.maxDeviatePoint(data(1))
//  println(max)
//  //println(ClusterRoutines.maxDeviatePoint(data(1)))
//  println(ClusterRoutines.distance(max,data(0)))
//  println(ClusterRoutines.distance(max,data(1).filter(_!=max)))
//
//  println(data(0).minBy(x => ClusterRoutines.distance(x, data(1).filter(_!=max))))

//  println(data.map(ClusterRoutines.meanDistToClosest).sum)
//  println(dataR.map(ClusterRoutines.meanDistToClosest).sum)
  //List(0.11544118281018706, 0.10786899567701902)
  //List(0.11333371848706895, 0.11724195858564224)
  var clusterer = new EqClusterer
  val clusters = clusterer.clusterize(data.flatMap(x => x), 3)

  println(clusters.map(ClusterRoutines.meanDistToClosest).toSeq.sortBy(x=>x))
  println(data.map(ClusterRoutines.meanDistToClosest).toSeq.sortBy(x=>x))

  FileWriter(clusters.map(c => c.sortBy(v => v.coords(0))).toList.sortBy(c => c(0).coords(0)).toString, false)

//  FileWriter(data.map(c => c.sortBy(v => v.coords(0))).toList.sortBy(c => c(0).coords(0)).toString)
//
  //val p = data(0)(2)
  //println(p)
  //val dists = (0 until data.length).map(j => data(j).map(y => y.distTo1(p)))
  //FileWriter(dists.toString)

  //ClusterRoutines.distance(p, data())


//  val gr1 = data(10)
//  val gr2 = data(7)
  //FileWriter(data(10).toString + "\n" + data(7).toString)
}
