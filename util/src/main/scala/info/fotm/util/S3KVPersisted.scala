package info.fotm.util

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectListing, ObjectMetadata, S3ObjectInputStream}
import com.amazonaws.util.IOUtils
import com.twitter.bijection.Bijection

import scala.collection.JavaConverters._
import scala.collection.breakOut
import scala.util.Try

class S3KVPersisted[K, V](bucket: String, keyPathBijection: Bijection[K, String])
                         (implicit valueSerializer: Bijection[V, Array[Byte]])
  extends Persisted[Map[K, V]] {

  val s3client = new AmazonS3Client()

  override def save(state: Map[K, V]): Try[Unit] = Try {
    for ((k, v) <- state) {
      val path: String = keyPathBijection(k)
      val bytes = valueSerializer(v)
      val stream = new ByteArrayInputStream(bytes)
      val meta = new ObjectMetadata()
      meta.setContentLength(bytes.length)
      s3client.putObject(bucket, path, stream, meta)
    }
  }

  override def fetch(): Try[Map[K, V]] = Try {
    val listing: ObjectListing = s3client.listObjects(bucket)
    val bucketEntries = listing.getObjectSummaries.asScala.toList
    val s3keys = bucketEntries.map(_.getKey)

    val result: Map[K, V] = (
      for (s3key <- s3keys) yield {
        val request = new GetObjectRequest(bucket, s3key)
        val s3object = s3client.getObject(request)
        val objectData: S3ObjectInputStream = s3object.getObjectContent
        val bytes = IOUtils.toByteArray(objectData)
        objectData.close()
        val k = keyPathBijection.inverse(s3key)
        val v = valueSerializer.inverse(bytes)
        (k, v)
      })(breakOut)

    result
  }
}
