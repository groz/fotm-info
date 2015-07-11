package info.fotm.util.azure

import com.twitter.bijection._
import info.fotm.util.Compression._
import scala.concurrent.Await

object AzureStorageApp extends App {

  def createStorage(container: String) = {
    implicit val str2rawGZipBase64 = Bijection.build[String, GZippedBase64String](s => GZippedBase64String(s))(_.str)
    implicit val keyIO: Bijection[String, String] = Bijection.identity[String]
    implicit val valueIO: Bijection[String, String] = str2GZippedBase64 andThen str2rawGZipBase64.inverse

    new AzureBlobStorage[String, String](AzureSettings.storageConnectionString, container)(keyIO, valueIO)
  }

  val azureStorage = createStorage("mytemp")

  val ops = for {
    putOp <- azureStorage.put("mykey" -> "clean json value")
    getOp <- azureStorage.get("mykey")
  } yield getOp

  val keys = azureStorage.keys("US/3v3")
  val valueFuture = azureStorage.get(keys.head)
  val value = Await.result(valueFuture, Inf)
  println(value)
}
