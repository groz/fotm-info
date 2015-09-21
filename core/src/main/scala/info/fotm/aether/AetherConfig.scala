package info.fotm.aether

import com.twitter.bijection.Bijection
import com.typesafe.config.{Config, ConfigFactory}
import info.fotm.util._

object AetherConfig {
  val baseConfig: Config = ConfigFactory.load()
  val configOverrideName = baseConfig.getString("config-override")

  println(s">>> Using config override: $configOverrideName")

  val config: Config = baseConfig.getConfig(configOverrideName).withFallback(baseConfig)

  val crawlerConfig = config.getConfig("crawler-system").withFallback(config)
  val portalConfig = config.getConfig("portal-system").withFallback(config)

  val dbPath = config.getString("mongodb.uri")
}
