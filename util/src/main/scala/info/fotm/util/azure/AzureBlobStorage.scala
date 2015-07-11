package info.fotm.util.azure

import java.util
import com.twitter.bijection.{Bijection}

import scala.collection.JavaConversions._
import com.microsoft.azure.storage._
import com.microsoft.azure.storage.blob._

import scala.concurrent.{Future, blocking}
import com.twitter.bijection.Conversion.asMethod

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
