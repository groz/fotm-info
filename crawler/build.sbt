import NativePackagerHelper._

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.codehaus.janino" % "janino" % "2.6.1",
  "org.codehaus.groovy" % "groovy" % "2.4.3",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT",
  "com.typesafe.akka" %% "akka-remote" % "2.4-SNAPSHOT",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4-SNAPSHOT",
  "com.typesafe.akka" %% "akka-testkit" % "2.4-SNAPSHOT" % "test"
)

javaOptions ++= Seq(
  "-XX:+UseConcMarkSweepGC",
  "-XX:+CMSClassUnloadingEnabled"
)

enablePlugins(JavaServerAppPackaging)

mainClass in Compile := Some("info.fotm.crawler.CrawlerApp")

mappings in Universal ++= {
  // optional example illustrating how to copy additional directory
  directory("scripts") ++
  // copy configuration files to config directory
  contentOf("src/main/resources").toMap.mapValues("config/" + _)
}

// add 'config' directory first in the classpath of the start script,
// an alternative is to set the config file locations via CLI parameters
// when starting the application
scriptClasspath := Seq("../config/") ++ scriptClasspath.value
