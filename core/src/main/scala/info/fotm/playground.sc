import info.fotm.util.MathVector
import scala.collection.GenIterable
import scala.collection.generic.CanBuildFrom
import scala.collection.breakOut
import scala.collection.immutable.IndexedSeq
import info.fotm.clustering.implementations._
  val x = new HTClusterer()
  val x11 = new MathVector(List(1.1, 1.0))
  val x12 = new MathVector(List(1.0, 1.1))
  val x13 = new MathVector(List(0.9, 1.1))
  val x21 = new MathVector(List(-1.1, 1.0))
  val x22 = new MathVector(List(-1.2, 0.9))
  val x23 = new MathVector(List(-1.0, 1.1))
  val x31 = new MathVector(List(1.1, -1.0))
  val x32 = new MathVector(List(1.0, -1.1))
  val x33 = new MathVector(List(1.0, -0.9))
  val x41 = new MathVector(List(-1.1, -0.9))
  val x42 = new MathVector(List(-1.0, -1.1))
  val x43 = new MathVector(List(-0.9, -1.1))
  val set = Seq(x11, x12, x13, x21, x22, x23, x31, x32, x33, x41, x42, x43)
  val r1 = x.clusterize(set, 3).map(x => x.sortBy(y => y(0) * 1000 + y(1)))
  var r2 = Set(Seq(x11, x12, x13), Seq(x21, x22, x23), Seq(x31, x32, x33), Seq(x41, x42, x43)).map(x => x.sortBy(y => y(0) * 1000 + y(1)))
  r1 == r2
/*
def batch[From[A] <: Traversable[A], A, To[_] <: Traversable[_]]
    (from: From[A], batchSize: Int)
    (implicit
     bf: CanBuildFrom[Traversable[A], To[A], To[To[A]]],
     wbf: CanBuildFrom[Traversable[A], A, To[A]])
  : To[To[A]] = {

  val b = bf()

  var size = 0
  var wb = wbf()

  from.foreach { a =>
    size += 1
    wb += a

    if (size == batchSize) {
      size = 0
      b += wb.result()
      wb.clear()
    }
  }

  if (size != 0)
    b += wb.result()

  b.result()
}

val lst: List[Int] = (0 to 10).toList

batch[List, Int, List](lst, 3)(breakOut, breakOut)
*/
