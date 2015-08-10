package info.fotm.util

import java.io.File

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{S3ObjectInputStream, GetObjectRequest, PutObjectRequest}
import com.twitter.bijection.Bijection

import scala.util.Try

class S3Persisted[T](bucket: String, path: String)(implicit serializer: Bijection[T, Array[Byte]]) extends Persisted[T] {

  val s3client = new AmazonS3Client()

  override def save(state: T): Unit = {
    val fileName = "tmpstorage.txt"
    val fileIO = new FilePersisted[T](fileName)(serializer)
    fileIO.save(state)

    val tmpFile = new File(fileName)
    s3client.putObject(new PutObjectRequest(bucket, path, tmpFile))
    tmpFile.delete()
  }

  override def fetch(): Option[T] = {
    val request = new GetObjectRequest(bucket, path)
    Try(s3client.getObject(request)).toOption.map { s3object =>
      val objectData: S3ObjectInputStream = s3object.getObjectContent
      val bytes = scala.io.Source.fromInputStream(objectData).mkString.getBytes
      objectData.close()
      serializer.inverse(bytes)
    }
  }
}
