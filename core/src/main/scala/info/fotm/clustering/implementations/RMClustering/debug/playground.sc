import info.fotm.clustering.Clusterer.Cluster
import info.fotm.clustering.implementations.RMClustering.ClusterRoutines._
import info.fotm.clustering.implementations.RMClustering.InputData
import info.fotm.util.MathVector


def meanDev(c: Cluster): Double =
{
  meanAbsDeviation(c, getClusterGeometricCenter(c))
}

val data = InputData.data


meanDev(InputData.data(0))
meanDev(InputData.results(0))
//val p = data(0)(2)

//val dists = (0 until data.length).map(j => data(j).map(y => y.distTo1(p)))



//(0 until data.length).map(
//  j => meanAbsDeviation(
//    data(j): Cluster,
//    getClusterGeometricCenter(data(j): Cluster)))
//  .sortBy(x => x)
