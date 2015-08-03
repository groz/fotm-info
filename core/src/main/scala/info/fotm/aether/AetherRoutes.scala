package info.fotm.aether

import com.typesafe.config.ConfigFactory

object AetherRoutes {
  val config = ConfigFactory.load()

  val crawlerSystem = config.getString("akka.crawlerSystem")
  val portalSystem = config.getString("akka.portalSystem")

  val storageActorName = "storage"
  val storageProxyActorName = "storage-proxy"

  val storagePath = s"$crawlerSystem/user/$storageActorName"
  val storageProxyPath = s"$crawlerSystem/user/$storageProxyActorName"
}
