lazy val commonSettings = Seq(
  organization := "info.fotm",
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.11.6"
)

name := "fotm"

lazy val root = (project in file(".")).settings(commonSettings: _*)
  .aggregate(bnetapi, util, core, crawler, portal)

lazy val bnetapi = project.settings(commonSettings: _*)

lazy val util = project.settings(commonSettings: _*)

lazy val core = project.dependsOn(util, bnetapi).settings(commonSettings: _*)

lazy val crawler = project.dependsOn(util, core, bnetapi).settings(commonSettings: _*)

lazy val portal = project.dependsOn(util, core, crawler, bnetapi).settings(commonSettings: _*).enablePlugins(PlayScala)

resolvers in ThisBuild += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

libraryDependencies in ThisBuild ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.codehaus.janino" % "janino" % "2.6.1",
  "org.codehaus.groovy" % "groovy" % "2.4.3"
)
