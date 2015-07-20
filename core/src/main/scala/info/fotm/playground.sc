import scala.collection.GenIterable
import scala.collection.generic.CanBuildFrom
import scala.collection.breakOut
import scala.collection.immutable.IndexedSeq

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
