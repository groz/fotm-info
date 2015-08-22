package info.fotm.util

import java.nio.file.{Files, Paths}

import com.twitter.bijection.Bijection

import scala.util.Try

class FilePersisted[S](fileName: String)(implicit serializer: Bijection[S, Array[Byte]]) extends Persisted[S] {

  override def save(state: S): Try[Unit] = Try {
    Files.write(Paths.get(fileName), serializer(state))
  }

  override def fetch(): Try[S] =
    Try {
      val bytes = Files.readAllBytes(Paths.get(fileName))
      serializer.inverse(bytes)
    }
}


