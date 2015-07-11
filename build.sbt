lazy val commonSettings = Seq(
  organization := "info.fotm",
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.11.6"
)

name := "fotm"

lazy val bnetapi = project.settings(commonSettings: _*)

lazy val util = project.settings(commonSettings: _*)

lazy val core = project.dependsOn(util, bnetapi).settings(commonSettings: _*)

lazy val crawler = project.dependsOn(util, core, bnetapi).settings(commonSettings: _*)

lazy val portal = project.dependsOn(util, core, crawler, bnetapi).settings(commonSettings: _*)
