resolvers += "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/"

libraryDependencies ++= Seq(
  "com.github.nscala-time" %% "nscala-time" % "2.0.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % Test,
  "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT",
  "com.typesafe.akka" %% "akka-remote" % "2.4-SNAPSHOT",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4-SNAPSHOT",
  "com.typesafe.akka" %% "akka-testkit" % "2.4-SNAPSHOT" % Test,
  "org.scodec" %% "scodec-bits" % "1.0.9",
  "org.scodec" %% "scodec-core" % "1.8.1"
)
