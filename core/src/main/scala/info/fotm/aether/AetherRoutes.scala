package info.fotm.aether

import com.typesafe.config.{Config, ConfigFactory}

case class SystemPathConfig(name: String, host: String, port: Int) {
  def this(config: Config, name: String) = this(
    name,
    config.getString("akka.remote.netty.tcp.hostname"),
    config.getInt("akka.remote.netty.tcp.port")
  )

  def locate(actorName: String): String = s"akka.tcp://$name@$host:$port/user/$actorName"
}

object AetherRoutes {
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
}
