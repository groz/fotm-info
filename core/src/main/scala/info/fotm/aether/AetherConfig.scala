package info.fotm.aether

import com.twitter.bijection.Bijection
import com.typesafe.config.{Config, ConfigFactory}
import info.fotm.util._

case class SystemPathConfig(name: String, host: String, port: Int) {
  def this(config: Config, name: String) = this(
    name,
    config.getString("akka.remote.netty.tcp.hostname"),
    config.getInt("akka.remote.netty.tcp.port")
  )

  def locate(actorName: String): String = s"akka.tcp://$name@$host:$port/user/$actorName"
}

object AetherConfig {
  val baseConfig: Config = ConfigFactory.load()
  val configOverrideName = baseConfig.getString("config-override")

  println(s">>> Using config override: $configOverrideName")

  val config: Config = baseConfig.getConfig(configOverrideName).withFallback(baseConfig)

  val crawlerConfig = config.getConfig("crawler-system").withFallback(config)
  val portalConfig = config.getConfig("portal-system").withFallback(config)

  val crawlerSystemPath = new SystemPathConfig(crawlerConfig, "crawler-system")
  val portalSystemPath = new SystemPathConfig(portalConfig, "portal-system")

  val storageActorName = "storage"
  val storageProxyActorName = "storage-proxy"

  val storagePath = crawlerSystemPath.locate(storageActorName)
  val storageProxyPath = portalSystemPath.locate(storageProxyActorName)

  val dbPath = config.getString("mongodb.uri")

  private val s3kvStorageKey = "s3-kvstorage"
  private val folderStorageKey = "folder-storage"

  def storagePersistence[K, V](
      implicit keyPathBijection: Bijection[K, String],
      valueSerializer: Bijection[V, Array[Byte]])
    : Persisted[Map[K, V]] = {

    val storageConfig = config.getConfig("storage")

    val result =
      if (storageConfig.hasPath(s3kvStorageKey)) {
        val s3cfg = storageConfig.getConfig(s3kvStorageKey)
        val bucket = s3cfg.getString("bucket")
        new S3KVPersisted[K, V](bucket, keyPathBijection)
      } else if (storageConfig.hasPath(folderStorageKey)) {
        val folderCfg = storageConfig.getConfig(folderStorageKey)
        val folder = folderCfg.getString("folder")
        val flatPath = keyPathBijection andThen Bijection.build[String, String](_.replace('/', '-'))(_.replace('-', '/'))
        new FolderPersisted[K, V](folder, flatPath)
      } else new NullPersisted[Map[K, V]]

    println(s">>> Using storage: $result")
    result
  }
}
