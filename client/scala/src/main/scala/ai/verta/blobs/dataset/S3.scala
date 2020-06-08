package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._

import collection.JavaConverters._

import scala.collection.mutable.HashMap
import scala.util.{Failure, Success, Try}
import scala.annotation.tailrec

/** Captures metadata about S3 objects
 *  Please set up AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, and AWS_REGION environment variables before use.
 *  To create a new instance, use the constructor taking a list of S3 Locations
 *  {{{
 *  val s3Blob: Try[S3] = S3(List(S3Location("some-path-1").get, S3Location("some-path-2").get))
 *  }}}
 */
case class S3(private val metadataList: List[Tuple2[String, FileMetadata]]) extends Dataset {
  protected var contents = HashMap(metadataList: _*)

  /** Get the version id of a file
   *  @param path: S3 URL of a file in the form "s3://<bucketName>/<key>"
   *  @return the version id of the file, if the file exists and has a versionId; otherwise None.
   */
  def getVersionId(path: String) = contents.get(path).flatMap(_.versionId)

  override def equals(other: Any) = other match {
    case other: S3 => contents.equals(other.contents)
    case _ => false
  }
}

/** Companion object to handle interaction with versioning blob */
object S3 {
  private val s3: AmazonS3 = AmazonS3ClientBuilder.standard().build()

  /** Constructor that user should use:
   */
  def apply(paths: List[S3Location]): Try[S3] = {
    val queryAttempt = Try(
      paths.map(getS3LocMetadata).map(_.get)
    ).map(_.flatten)

    queryAttempt match {
      case Failure(e) => Failure(e)
      case Success(list) => Success(new S3(list.map(metadata => metadata.path -> metadata)))
    }
  }

  /** Factory method to convert a versioning blob instance
   *  @param s3VersioningBlob the versioning blob to convert
   */
  def apply(s3VersioningBlob: VersioningS3DatasetBlob) {
    var s3Blob = new S3(List())
    val componentList = s3VersioningBlob.components.get
    val metadataList = componentList.map(
      comp => comp.path.get.path.get -> s3Blob.toMetadata(comp.path.get, comp.s3_version_id)
    )

    s3Blob
  }

  /** Convert a S3 instance to a VersioningBlob
   *  @param blob S3 instance
   *  @return equivalent VersioningBlob instance
   */
  def toVersioningBlob(blob: S3) = {
    val s3Components = blob.components.map(comp => VersioningS3DatasetComponentBlob(
      Some(comp),
      blob.getVersionId(comp.path.get)
    ))

    VersioningBlob(
      dataset = Some(VersioningDatasetBlob(
        s3 = Some(VersioningS3DatasetBlob(Some(s3Components)))
      ))
    )
  }

  /** Helper function to query metadata of S3 Location
   */
  private def getS3LocMetadata(loc: S3Location) = {
    if (loc.key.isEmpty || loc.key.get.endsWith("/")) {
      // no key (bucket), or is a folder
      val versionListing = s3.listVersions(loc.bucketName, loc.key.orNull)
      handleVersionListing(versionListing, List()).map(_.reverse)
    }
    else {
      val request =
        if (loc.versionID.isDefined) new GetObjectMetadataRequest(loc.bucketName, loc.key.get, loc.versionID.get)
        else new GetObjectMetadataRequest(loc.bucketName, loc.key.get)

      getObjectMetadata(s3.getObjectMetadata(request), loc.bucketName, loc.key.get).map(List(_))
    }
  }

  /** Helper function to deal with VersionListing
   */
  @tailrec private def handleVersionListing(
    versionListing: VersionListing,
    acc: List[FileMetadata]
  ): Try[List[FileMetadata]] = {
    val batchAttempt = Try(
      versionListing.getVersionSummaries().asScala.toList
                    .filter((version: S3VersionSummary) => !version.getKey().endsWith("/")) // not a folder
                    .filter(_.isLatest())
                    .map(getVersionMetadata) // List[Try]
                    .reverse
                    .map(_.get)
      )

    batchAttempt match {
      case Failure(e) => Failure(e)
      case Success(batch) =>
        if (versionListing.isTruncated())
          handleVersionListing(s3.listNextBatchOfVersions(versionListing), batch ::: acc)
        else Success(batch ::: acc)
    }
  }

  /** Helper function to extract metadata from the return object
   */
  private def getObjectMetadata(obj: ObjectMetadata, bucketName: String, key: String) = Try {
    new FileMetadata(
      BigInt(obj.getLastModified().getTime()), // convert time to UNIX timestamp (ms)
      obj.getETag(),
      getPath(bucketName, key),
      BigInt(obj.getContentLength()),
      Option(obj.getVersionId())
    )
  }

  /** Helper function to extract metadata from a version summary
   */
  private def getVersionMetadata(version: S3VersionSummary) = Try {
    new FileMetadata(
      BigInt(version.getLastModified().getTime()),
      version.getETag(),
      getPath(version.getBucketName(), version.getKey()),
      BigInt(version.getSize()),
      Option(version.getVersionId())
    )
  }

  /** Helper function to construct path from bucket name and key */
  private def getPath(bucketName: String, key: String) = f"s3://${bucketName}/${key}"
}
