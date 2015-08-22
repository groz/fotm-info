package info.fotm.util

import java.nio.file.{Files, Path, Paths}

import com.twitter.bijection.Bijection

import scala.collection.JavaConverters._
import scala.util.Try

// NB! Only supports one level under folder
class FolderPersisted[K, V](folder: String, keyPathBijection: Bijection[K, String])
                           (implicit valueSerializer: Bijection[V, Array[Byte]])
  extends Persisted[Map[K, V]] {

  override def save(state: Map[K, V]): Try[Unit] = Try {
    val rootPath = Paths.get(folder)

    if (!Files.exists(rootPath)) {
      Files.createDirectories(rootPath)
    }

    for ((k, v) <- state) {
      val key: String = keyPathBijection(k)
      val filePath: Path = rootPath.resolve(Paths.get(key))
      Files.createDirectories(filePath.getParent)
      Files.write(filePath, valueSerializer(v))
    }
  }

  override def fetch(): Try[Map[K, V]] = Try {
    val rootPath = Paths.get(folder)
    val files: List[Path] = Files.newDirectoryStream(rootPath).asScala.toList
    (for (filePath <- files) yield {
      val bytes = Files.readAllBytes(filePath)
      val v = valueSerializer.inverse(bytes)
      val k = keyPathBijection.inverse(filePath.getFileName.toString)
      (k, v)
    }).toMap
  }
}
