package info.fotm.util.azure

import com.microsoft.windowsazure.services.servicebus.models._
import scala.util.Try

import java.util
import com.microsoft.windowsazure.services.servicebus.ServiceBusService
import com.twitter.bijection.{GZippedBase64String, Bijection}
import info.fotm.util.Compression

import scala.collection.JavaConversions._
import com.microsoft.azure.storage._
import com.microsoft.azure.storage.blob._

import scala.concurrent.duration.Duration._
import scala.concurrent.{Await, Future, blocking}
import com.twitter.bijection.Conversion.asMethod
import scala.collection.JavaConverters._
import Compression._

import scala.concurrent.ExecutionContext.Implicits.global

trait ReadableStore[-K, +V] {
  def get(k: K): Future[Option[V]] = multiGet(Set(k))(k)
  def multiGet[K1 <: K](ks: Set[K1]): Map[K1, Future[Option[V]]] = ks.map { k => (k, get(k)) }.toMap
}

trait WritableStore[-K, -V] {
  def put(kv: (K, V)): Future[Unit] = multiPut(Map(kv))(kv._1)
  def multiPut[K1 <: K](kvs: Map[K1, V]): Map[K1, Future[Unit]] = kvs.map { kv => (kv._1, put(kv)) }
}

trait Store[-K, V] extends ReadableStore[K, V] with WritableStore[K, V]

class AzureBlobStorage[K, V](connectionString: String, containerName: String)
  (implicit val keyIO: Bijection[K, String], implicit val valueIO: Bijection[V, String])
  extends Store[K, V] {

  val account: CloudStorageAccount = CloudStorageAccount.parse(connectionString)
  val serviceClient: CloudBlobClient = account.createCloudBlobClient()

  // Container name must be lower case.
  val container: CloudBlobContainer = serviceClient.getContainerReference(containerName)
  container.createIfNotExists()

  override def get(k: K): Future[Option[V]] = {
    val key = k.as[String]
    println(key)
    val blob: CloudBlockBlob = container.getBlockBlobReference(key)

    if (!blob.exists())
      Future.successful(None)
    else Future {
      val str = blocking { blob.downloadText() }
      Some(valueIO.inverse(str))
    }
  }

  override def put(kv: (K, V)): Future[Unit] = {
    val (k, v) = kv

    val key = k.as[String]
    val blob = container.getBlockBlobReference(key)

    val value = v.as[String]
    Future {
      blocking { blob.uploadText(value) }
    }
  }

  def keys(directory: String = "") = {
    val bs = container.listBlobs(directory, true, util.EnumSet.noneOf(classOf[BlobListingDetails]), null, null).view
    bs.collect { case b: CloudBlockBlob => keyIO.inverse(b.getName) } // skip directories
    //this.allBlobs(directory) |> Array.map (fun blob -> blob.Uri)
  }
}

object AzureStorageApp extends App {

  val storageConnectionString =
    ""

  def createStorage(container: String) = {
    implicit val str2rawGZipBase64 = Bijection.build[String, GZippedBase64String](s => GZippedBase64String(s))(_.str)
    implicit val keyIO: Bijection[String, String] = Bijection.identity[String]
    implicit val valueIO: Bijection[String, String] = str2GZippedBase64 andThen str2rawGZipBase64.inverse

    new AzureBlobStorage[String, String](storageConnectionString, container)(keyIO, valueIO)
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

object AzureServiceBusApp extends App {
  val config =
    com.microsoft.windowsazure.services.servicebus.ServiceBusConfiguration.configureWithSASAuthentication(
      "fotm-servicebus",            // namespace
      "RootManageSharedAccessKey",  // sasKeyName
      "",   // sasKey
      ".servicebus.windows.net"     // serviceBusRootUri
    )

  val topicName = "team-updates"

  val serviceBus = ServiceBusService.create(config)

  val subName = "AllMessages"
  val subInfo: SubscriptionInfo =
    Try(serviceBus.createSubscription(topicName, new SubscriptionInfo(subName)).getValue)
    .getOrElse(serviceBus.getSubscription(topicName, subName).getValue)

  val opts = ReceiveMessageOptions.DEFAULT.setReceiveMode(ReceiveMode.PEEK_LOCK)

  while (true) {
    println(s"Count: ${subInfo.getCountDetails.getActiveMessageCount}")

    val resultSubMsg = serviceBus.receiveSubscriptionMessage(topicName, subName, opts)
    val messageOption = Option(resultSubMsg.getValue)

    for (message <- messageOption) {
      println("MessageID: " + message.getMessageId)
      val res = scala.io.Source.fromInputStream(message.getBody, "UTF-8")
      println(res.mkString)
      //serviceBus.deleteMessage(message)
    }
    readLine()
  }
}
