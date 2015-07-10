libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "18.0",
  "com.twitter" %% "bijection-core" % "0.8.1",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

// Azure core

libraryDependencies += "com.microsoft.azure" % "azure-core" % "0.7.0"

// Azure storage

libraryDependencies += "com.microsoft.azure" % "azure-storage" % "2.2.0"

// Azure servicebus

libraryDependencies ++= Seq(
  "com.microsoft.azure" % "azure-servicebus" % "0.7.0",
  "com.sun.jersey" % "jersey-core" % "1.19"
  //"com.sun.jersey" % "jersey-client" % "1.19",
  //"com.sun.jersey" % "jersey-json" % "1.19"
)
