package info.fotm.util

import java.nio.file.{Files, Paths}

import com.twitter.bijection.Bijection

class FilePersisted[S](fileName: String)(implicit serializer: Bijection[S, Array[Byte]]) extends Persisted[S] {

  override def save(state: S): Unit = {
    Files.write(Paths.get(fileName), serializer(state))
  }

  override def fetch(): Option[S] =
    if (Files.exists(Paths.get(fileName))) {
      val bytes = Files.readAllBytes(Paths.get(fileName))
      Some(serializer.inverse(bytes))
    } else {
      None
    }
}
