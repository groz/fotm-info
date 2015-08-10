package info.fotm.aether

import com.twitter.bijection.Bijection
import com.typesafe.config.{Config, ConfigFactory}
import info.fotm.util.{S3Persisted, FilePersisted, NullPersisted}

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

  val config = baseConfig.getConfig(configOverrideName).withFallback(baseConfig)

  val crawlerConfig = config.getConfig("crawler-system").withFallback(config)
  val portalConfig = config.getConfig("portal-system").withFallback(config)

  val crawlerSystemPath = new SystemPathConfig(crawlerConfig, "crawler-system")
  val portalSystemPath = new SystemPathConfig(portalConfig, "portal-system")

  val storageActorName = "storage"
  val storageProxyActorName = "storage-proxy"

  val storagePath = crawlerSystemPath.locate(storageActorName)
  val storageProxyPath = portalSystemPath.locate(storageProxyActorName)

  private val fileStorageKey = "file-storage"
  private val s3StorageKey = "s3-storage"

  def storagePersistence[T](implicit serializer: Bijection[T, Array[Byte]]) = {
    val storageConfig = config.getConfig("storage")

    val result = if (storageConfig.hasPath(fileStorageKey)) {
      val path = storageConfig.getConfig(fileStorageKey).getString("path")
      new FilePersisted[T](path)
    } else if (storageConfig.hasPath(s3StorageKey)) {
      val s3cfg = storageConfig.getConfig(s3StorageKey)
      val bucket = s3cfg.getString("bucket")
      val path = s3cfg.getString("path")
      new S3Persisted[T](bucket, path)
    } else new NullPersisted[T]

    println(s">>> Using storage: $result")
    result
  }
}
