package info.fotm.aether

import com.typesafe.config.{Config, ConfigFactory}

case class SystemPath(name: String, host: String, port: Int) {
  def this(config: Config, name: String) = this(
    name,
    config.getString(s"${name}Config.akka.remote.netty.tcp.hostname"),
    config.getInt(s"${name}Config.akka.remote.netty.tcp.port")
  )

  def locate(actorName: String): String = s"akka.tcp://$name@$host:$port/user/$actorName"
}

object AetherRoutes {
  val config: Config = ConfigFactory.load()

  val configOverride = config.getString("configOverride")
  println(s"*** Using config override: $configOverride...")

  val crawlerSystemPath = new SystemPath(config, s"$configOverride.crawlerActorSystem")
  val portalSystemPath = new SystemPath(config, s"$configOverride.portalActorSystem")

  val storageActorName = "storage"
  val storageProxyActorName = "storage-proxy"

  val storagePath = crawlerSystemPath.locate(storageActorName)
  val storageProxyPath = portalSystemPath.locate(storageProxyActorName)
}
