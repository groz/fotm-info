lazy val commonSettings = Seq(
  organization := "info.fotm",
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  test in assembly := {}
)

name := "fotm"

lazy val root = (project in file(".")).settings(commonSettings: _*)
  .aggregate(bnetapi, util, core, crawler, portal)

lazy val bnetapi = project.settings(commonSettings: _*)

lazy val util = project.settings(commonSettings: _*)

lazy val core = project.dependsOn(util, bnetapi).settings(commonSettings: _*)

lazy val crawler = project.dependsOn(util, core, bnetapi).settings(commonSettings: _*)

lazy val portal = project.dependsOn(util, core, bnetapi).settings(commonSettings: _*).enablePlugins(PlayScala)

resolvers in ThisBuild += "Akka Snapshots" at "http://repo.akka.io/snapshots/"

resolvers in ThisBuild += "Typesafe Snapshots" at "https://repo.typesafe.com/typesafe/snapshots/"
