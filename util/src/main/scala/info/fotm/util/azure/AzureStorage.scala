package info.fotm.util.azure

import java.io._
import java.util
import java.util.zip.GZIPInputStream
import java.util.{EnumSet, Calendar, Date}
import com.twitter.bijection.{GZippedBase64String, Base64String, Bijection}
import info.fotm.util.Compression

import scala.collection.JavaConversions._
import com.microsoft.azure.storage._
import com.microsoft.azure.storage.blob._

import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

import scala.concurrent.duration.Duration._
import scala.concurrent.{Await, Future}
import scala.io.BufferedSource
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
      val v = valueIO.inverse(blob.downloadText())
      Some(v)
    }
  }

  override def put(kv: (K, V)): Future[Unit] = {
    val (k, v) = kv

    val key = k.as[String]
    val blob = container.getBlockBlobReference(key)

    val value = v.as[String]
    Future {
      blob.uploadText(value)
    }
  }

  lazy val keys = {
    for {
      blob: ListBlobItem <- container.listBlobs(null, true, util.EnumSet.noneOf(classOf[BlobListingDetails]), null, null).view
    } yield blob.asInstanceOf[CloudBlob].getName
  }
}

object AzureStorageApp extends App {

  val key = "CN/2v2/00017398-3382-42c1-963d-efe6a543bf80"

  val storageConnectionString =
    ""

  implicit val str2rawGZipBase64 = Bijection.build[String, GZippedBase64String](s => GZippedBase64String(s))(_.str)
  implicit val keyIO: Bijection[String, String] = Bijection.identity[String]
  implicit val valueIO: Bijection[String, String] = str2GZippedBase64 andThen str2rawGZipBase64.inverse

  val azureStorage = new AzureBlobStorage[String, String](storageConnectionString, "snapshots")(keyIO, valueIO)

  val valueFuture = azureStorage.get(key)
  val value = Await.result(valueFuture, Inf)

  azureStorage.keys.foreach(println)

/*
  val account: CloudStorageAccount = CloudStorageAccount.parse(storageConnectionString)
  val serviceClient: CloudBlobClient = account.createCloudBlobClient()

  // Container name must be lower case.
  val container: CloudBlobContainer = serviceClient.getContainerReference("snapshots")

  def getDir(path: String) = container.listBlobs(path).toSeq.head.asInstanceOf[CloudBlobDirectory]

  println("Connected...")

  val root: CloudBlobDirectory = getDir("EU/3v3")
  // CloudBlob, CloudBlobDirectory

  val blobs = for {
    item <- root.listBlobs.view
    blob = item.asInstanceOf[CloudBlob]
  } yield blob

  val snapshotBlob: CloudBlob = blobs.take(1).toList.head
  println("Downloading file...")
  //snapshotBlob.downloadToFile("1.txt")
  println("Reading file...")
  val base64zip: String = io.Source.fromFile("1.txt").mkString
  println("Uncompressing...")
  val uncompressed: String = Bijection.invert[String, GZippedBase64String](GZippedBase64String(base64zip)) //Compression.unzipFromBase64(base64zip)
  println(uncompressed)

  def printForLastMonth = {
    val monthAgo = {
      val cal = Calendar.getInstance()
      cal.add(Calendar.MONTH, -1)
      cal.getTime()
    }

    var i = 0

    println("Listing blobs...")
    val lastMonth = for {
      item <- root.listBlobs.view
      blob = item.asInstanceOf[CloudBlob]
      props = blob.getProperties if props.getLastModified.after(monthAgo)
    } yield (blob.getUri, props.getLastModified)

    println("Printing...")
    lastMonth.foreach { case (blobUri, blobModified) =>
      println(blobUri, blobModified)
      i += 1
      println(i)
    }
  }

  //val blob: CloudBlockBlob = container.getBlockBlobReference("image1.jpg")
  //val destinationFile: File = new File(sourceFile.getParentFile(), "image1Download.tmp")
  //blob.downloadToFile(destinationFile.getAbsolutePath())
  */
}
