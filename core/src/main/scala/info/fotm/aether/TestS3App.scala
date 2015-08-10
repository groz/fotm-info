package info.fotm.aether

import info.fotm.util.Compression

object TestS3App extends App {
  val pers = AetherConfig.storagePersistence[String](Compression.str2bytes)
  pers.fetch().foreach(println)
}
