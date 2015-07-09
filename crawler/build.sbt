libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.codehaus.janino" % "janino" % "2.6.1",
  "org.codehaus.groovy" % "groovy" % "2.4.3",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

resolvers += "Typesafe Snapshots" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT",
  "com.typesafe.akka" %% "akka-remote" % "2.4-SNAPSHOT",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4-SNAPSHOT",
  "com.typesafe.akka" %% "akka-testkit" % "2.4-SNAPSHOT" % "test"
)
