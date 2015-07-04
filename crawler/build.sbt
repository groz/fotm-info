libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.4.0",
  "com.typesafe.play" %% "play-ws" % "2.4.0"
)

resolvers += "Typesafe Snapshots" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT",
  "com.typesafe.akka" %% "akka-remote" % "2.4-SNAPSHOT"
)
