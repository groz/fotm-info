package info.fotm.util

import java.io.{ObjectOutputStream, ByteArrayOutputStream}

object Inspection {
  def serializedSize[T <: Serializable](obj: T): Int = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(obj)
    oos.close()
    baos.size()
  }
}
