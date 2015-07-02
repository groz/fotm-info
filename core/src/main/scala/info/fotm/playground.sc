import info.fotm.util.MathVector

trait Binding[T] {
  val boundValue: T
}

trait Binding2[A, B] {
  val a: A
  val b: B
}

def bind[T](v: MathVector, value: T): MathVector with Binding[T] =
  new MathVector(v.coords) with Binding[T] {
    val boundValue = value
  }

val x: Option[Int] = Some(10)

x.fold(false)(_ > 5)