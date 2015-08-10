import java.io.File
import com.typesafe.config.ConfigFactory
import info.fotm.aether.AetherConfig

import play.api.ApplicationLoader
import play.api.Configuration
import play.api.inject._
import play.api.inject.guice._

class CustomApplicationLoader extends GuiceApplicationLoader() {
  override def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder = {
    initialBuilder
      .in(context.environment)
      .loadConfig(context.initialConfiguration ++ Configuration(AetherConfig.portalConfig))
      .overrides(overrides(context): _*)
  }
}
