package info.fotm.clustering.implementations.RMClustering

import info.fotm.clustering.implementations.RMClustering.ClusterRoutines._
import info.fotm.clustering.Clusterer._

/**
 * Created by Admin on 05.07.2015.
 */
class FineTuner(groupSize: Int)
{

  case class LabeledCluster(cluster: Cluster, triedWith: Set[Cluster] = Set())
  {
    val excludeClusters = triedWith + cluster
  }

  def fineTuning2(clusters: Set[Cluster]): Set[Cluster] =
  {
    fineTuningStep2(clusters.map(LabeledCluster(_))).map(_.cluster)

  }

  def fineTuningStep2(labClusters: Set[LabeledCluster]): Set[LabeledCluster] =
  {
    val devPointsMap = labClusters.map(c => c -> maxDeviatePoint(c.cluster)).toMap
    val clusters = labClusters.map(_.cluster)
    val devClosestClusters = devPointsMap.map(x =>
      x._2 -> closestClusterAmong(clusters -- x._1.excludeClusters, x._2))
    val testedPoints = devPointsMap.filterNot(p => devClosestClusters(p._2).isEmpty);

    if (testedPoints.isEmpty)
      labClusters
    else
    {
      val (devPoint, devCluster, distToClosestCluster) =
        testedPoints.map(x => (x._2, x._1,
          distance(x._2, (x._1.cluster.toSet - x._2).toSeq) -
            distance(x._2, devClosestClusters(x._2))
          )
        ).maxBy(y => (y._2.cluster.length, y._3))

      if (devCluster.cluster.length > groupSize && distToClosestCluster < 0)
      {
        // drop this suspicious item from group
        fineTuningStep2(labClusters - devCluster + (LabeledCluster(devCluster.cluster.filter(_ != devPoint))))
      }
      else if (distToClosestCluster >= 0)
      {
        val (x, y) = (devCluster.cluster, devClosestClusters(devPoint))
        val (u, v) = movePoint(devPoint, x, y)
        // if nothing changed, we mark cluster x to exclude it from consideration
        if ((x, y) ==(u, v))
          fineTuningStep2(labClusters.map(c => if (c.cluster == x) LabeledCluster(x, c.excludeClusters + y) else c))
        else
          fineTuningStep2(
            labClusters
              -- Set(devCluster, labClusters.find(_.cluster == y).get)
              ++ Set(LabeledCluster(u), LabeledCluster(v))
          )
      }
      else
        labClusters
    }
  }

  def movePoint(p: V, from: Cluster, to: Cluster): (Cluster, Cluster) =
  {
    val newCluster1 = from.filter(_ != p)
    val newCluster2 = to :+ p
    if (from.length > groupSize ||
      Seq(newCluster1, newCluster2).map(meanDistToClosest).sum <
        Seq(from, to).map(meanDistToClosest).sum)
      (newCluster1, newCluster2)
    else
      (from, to)
  }


  //================================

  def fineTuning(clusters: Set[Cluster]): Set[Cluster] =
  {
    fineTuningStep(clusters.map(LabeledCluster(_))).map(_.cluster)
  }

  def fineTuningStep(labClusters: Set[LabeledCluster]): Set[LabeledCluster] =
  {
    val devPointsMap = labClusters.map(c => c -> maxDeviatePoint(c.cluster)).toMap
    val clusters = labClusters.map(_.cluster)
    val devClosestClusters = devPointsMap.map(x =>
      x._2 -> closestClusterAmong(clusters -- x._1.excludeClusters, x._2))

    val testedPoints = devPointsMap.filterNot(p => devClosestClusters(p._2).isEmpty);

    if (testedPoints.isEmpty)
      labClusters
    else
    {


      val (devPoint, devCluster, distToClosestCluster) =
        testedPoints.map(x => (x._2, x._1,
          distance(x._2, (x._1.cluster.toSet - x._2).toSeq) - distance(x._2, devClosestClusters(x._2))
          )
        ).maxBy(y => y._3)

      if (distToClosestCluster >= 0)
      {
        val (x, y) = (devCluster.cluster, devClosestClusters(devPoint))
        val (u, v) = exchangePoint(x, y)
        // if nothing changed, we mark cluster x to exclude it from consideration
        if ((x, y) ==(u, v))
          fineTuningStep(labClusters.map(c => if (c.cluster == x) LabeledCluster(x, c.excludeClusters + y) else c))
        else
          fineTuningStep(
            labClusters
              -- Set(devCluster, labClusters.find(_.cluster == y).get)
              ++ Set(LabeledCluster(u), LabeledCluster(v))
          )
      }
      else
        labClusters
    }
  }

  def maxDeviatePoint(cluster: Cluster): V =
  {
    cluster.maxBy(x => distance(x, (cluster.toSet - x).toSeq))
  }

  def closestClusterAmong(clusters: Set[Cluster], to: V): Cluster =
  {
    if (clusters.isEmpty)
      Seq()
    else
      clusters.minBy(distance(to, _))
  }

  def exchangePoint(withDevPoint: Cluster, other: Cluster): (Cluster, Cluster) =
  {
    val devPoint = maxDeviatePoint(withDevPoint)
    val withoutDevPoint = withDevPoint.filter(p => p != devPoint)
    val fromOther = closestVector(other, withoutDevPoint)

    // exchange
    val newCluster1 = withoutDevPoint :+ fromOther
    val newCluster2 = other.filter(_ != fromOther) :+ devPoint

    // check deviation
    if (Seq(newCluster1, newCluster2).map(meanDistToClosest).sum <
      Seq(withDevPoint, other).map(meanDistToClosest).sum)
      (newCluster1, newCluster2)
    else
      (withDevPoint, other)
  }

}
