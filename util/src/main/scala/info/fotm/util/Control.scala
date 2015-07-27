package info.fotm.util

object Control {

  def using[A <: AutoCloseable, B] (param: A) (f: A => B): B =
    try {
      f(param)
    } finally {
      param.close()
    }
}
