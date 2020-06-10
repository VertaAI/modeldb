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
 *  To create a new instance, use the constructor taking a list of S3 Locations or a single location
 *  {{{
 *  val s3Blob: Try[S3] = S3(List(S3Location("some-path-1").get, S3Location("some-path-2").get))
 *  val s3Blob2: Try[S3] = S3(S3Location("some-path"))
 *  }}}
 */
case class S3(protected val contents: HashMap[String, FileMetadata]) extends Dataset {
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

  /** Constructor taking only one S3Location
   *  @param location a single S3Location
   *  @return if location is invalid, a failure along with exception message. Otherwise, the blob (wrapped in success)
   */
  def apply(location: S3Location): Try[S3] = apply(List(location))

  /** Constructor that user should use.
  *  @return if any location is invalid, a failure along with exception message. Otherwise, the blob (wrapped in success)
   */
  def apply(paths: List[S3Location]): Try[S3] = {
    val queryAttempt = Try(
      paths.map(getS3LocMetadata).map(_.get)
    ).map(_.flatten)

    queryAttempt match {
      case Failure(e) => Failure(e)
      case Success(list) => Success(new S3(HashMap(list.map(metadata => metadata.path -> metadata): _*)))
    }
  }

  /** Factory method to convert a versioning blob instance
   *  @param s3VersioningBlob the versioning blob to convert
   */
  def apply(s3VersioningBlob: VersioningS3DatasetBlob) = {
    val componentList = s3VersioningBlob.components.get
    val metadataList = componentList.map(
      comp => comp.path.get.path.get -> Dataset.toMetadata(comp.path.get, comp.s3_version_id)
    )

    new S3(HashMap(metadataList: _*))
  }

  /** Combine two S3 instances
   *  @param firstBlob: first S3 blob
   *  @param secondBlob: second S3 blob
   *  @return failure if the two blobs have conflicting entries; the combined blob otherwise.
   */
  def reduce(firstBlob: S3, secondBlob: S3): Try[S3] = {
    if (firstBlob.notConflicts(secondBlob))
      Success(new S3(firstBlob.contents ++ secondBlob.contents))
    else Failure(new IllegalArgumentException("The two blobs have conflicting entries"))
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
      handleVersionListing(versionListing, List()).map(_.reverse) // list was originally reversed
    }
    else {
      val request =
        if (loc.versionID.isDefined) new GetObjectMetadataRequest(loc.bucketName, loc.key.get, loc.versionID.get)
        else new GetObjectMetadataRequest(loc.bucketName, loc.key.get)

      getObjectMetadata(s3.getObjectMetadata(request), loc.bucketName, loc.key.get).map(List(_))
    }
  }

  /** Helper function to deal with VersionListing and extract metadata from the listing
   *  Handle truncated listing via (tail) recursion
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
                    .reverse // reverse the batch
                    .map(_.get)
      )

    batchAttempt match {
      case Failure(e) => Failure(e)
      case Success(batch) =>
        if (versionListing.isTruncated())
          handleVersionListing(s3.listNextBatchOfVersions(versionListing), batch ::: acc)
        else Success(batch ::: acc) // concat order is for efficiency purpose.
        // whole list will be reversed later
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
