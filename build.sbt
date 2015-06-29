lazy val commonSettings = Seq(
  organization := "com.example",
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.11.6"
)

name := "fotm"

lazy val util = project.settings(commonSettings: _*)

lazy val core = project.dependsOn(util).settings(commonSettings: _*)

lazy val crawler = project.dependsOn(util, core).settings(commonSettings: _*)
