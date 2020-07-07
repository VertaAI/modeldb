package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.repository.Commit

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._

import collection.JavaConverters._

import scala.collection.mutable.HashMap
import scala.util.{Failure, Success, Try}
import scala.annotation.tailrec

import java.io.{File, FileOutputStream}

/** Captures metadata about S3 objects
 *  Please set up AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, and AWS_REGION environment variables before use.
 *  To create a new instance, use the constructor taking a list of S3 Locations or a single location
 *  {{{
 *  val s3Blob: Try[S3] = S3(List(S3Location("some-path-1").get, S3Location("some-path-2").get))
 *  val s3Blob2: Try[S3] = S3(S3Location("some-path"))
 *  }}}
 */
case class S3(
  protected val contents: HashMap[String, FileMetadata],
  val enableMDBVersioning: Boolean = false
) extends Dataset {
  /** Get the version id of a file
   *  @param path: S3 URL of a file in the form "s3://<bucketName>/<key>"
   *  @return the version id of the file, if the file exists and has a versionId; otherwise None.
   */
  def getVersionId(path: String) = contents.get(path).flatMap(_.versionId)

  /** Preparing for uploading by downloading the stored objects from S3 and update metadata
   *  @return Whether the attempt succeeds
   */
  override def prepareForUpload(): Try[Unit] = {
    if (enableMDBVersioning)
      Try(
        contents
          .values
          .map(metadata => downloadFromS3(S3Location(metadata.path, metadata.versionId).get, S3.s3, metadata).get)
      )
    else Success(())
  }

  /** Helper method to download the object stored in a S3 location and update the metadata
   *  Based on https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-s3-objects.html#download-object
   *  @param location the S3 location. Must correspond to a file
   *  @param s3 The s3 client
   *  @param metadata the FileMetadata instance
   *  @return whether the attempt succeeds.
   */
  private def downloadFromS3(location: S3Location, s3: AmazonS3, metadata: FileMetadata) = Try {
    // follow the pattern given here:
    // https://alvinalexander.com/scala/how-declare-variable-option-before-try-catch-finally-scala/
    // finally block cannot refer to variable declared in try block
    var s3InputStream: Option[S3ObjectInputStream] = None
    var fileOutputStream: Option[FileOutputStream] = None

    try {
      val file = File.createTempFile(location.key.get, null)

      val request =
        if(location.versionID.isDefined)
          new GetObjectRequest(location.bucketName, location.key.get, location.versionID.get)
        else
          new GetObjectRequest(location.bucketName, location.key.get)
      val obj: S3Object = s3.getObject(request)

      s3InputStream = Some(obj.getObjectContent())
      fileOutputStream = Some(new FileOutputStream(file))

      val BufferSize = 1024 * 1024 // 1 MB
      val buffer = new Array[Byte](BufferSize)

      var readLen = s3InputStream.get.read(buffer)
      while (readLen > 0) {
          fileOutputStream.get.write(buffer, 0, readLen)
          readLen = s3InputStream.get.read(buffer)
      }

      val internalVersionedPath = f"${Dataset.hash(file, "SHA-256").get}/${location.key.get}"

      /** TODO: remove these  */
      // mutating internal field:
      metadata.internalVersionedPath = Some(internalVersionedPath)
      metadata.localPath = Some(file.getPath)
    } finally {
      if (s3InputStream.isDefined) s3InputStream.get.close()
      if (fileOutputStream.isDefined) fileOutputStream.get.close()
    }
  }

  /** Deletes temporary files that had been downloaded for ModelDB-managed versioning
   *  This method does nothing if ModelDB-managed versioning was not enabled.
   *  @return whether the delete attempts succeeds.
   */
  override private[verta] def cleanUpUploadedComponents(): Try[Unit] = {
    if (enableMDBVersioning)
      Try(contents.values.map(_.localPath.get).map(path => Try((new File(path)).delete())).map(_.get))
    else
      Success(())
  }

  override def equals(other: Any) = other match {
    case other: S3 => contents.equals(other.contents)
    case _ => false
  }
}

/** Companion object to handle interaction with versioning blob */
object S3 {
  private val s3: AmazonS3 = AmazonS3ClientBuilder.standard().build()

  /** Constructor taking only one S3Location. Does not version with ModelDB.
   *  @param location a single S3Location
   *  @return if location is invalid, a failure along with exception message. Otherwise, the blob (wrapped in success)
   */
  def apply(location: S3Location): Try[S3] = apply(List(location))

  /** Constructor taking a list of S3 locations. Does not version with ModelDB.
   *  @param locations list of locations
   *  @return if any location is invalid, a failure along with exception message. Otherwise, the blob (wrapped in success)
   */
  def apply(locations: List[S3Location]): Try[S3] = apply(locations, false)

  /** Constructor taking only one S3 location.
   *  @param location a single S3Location
   *  @param enableMDBVersioning whether to version the data in the blob
   *  @return if any location is invalid, a failure along with exception message. Otherwise, the blob (wrapped in success)
   */
  def apply(location: S3Location, enableMDBVersioning: Boolean): Try[S3] = apply(List(location), enableMDBVersioning)

  /** Constructor taking a list of S3 locations.
   *  @param locations list of locations
   *  @param enableMDBVersioning whether to version the data in the blob
   *  @return if any location is invalid, a failure along with exception message. Otherwise, the blob (wrapped in success)
   */
  def apply(locations: List[S3Location], enableMDBVersioning: Boolean): Try[S3] = {
    val queryAttempt = Try(
      locations.map(getS3LocMetadata).map(_.get)
    ).map(_.flatten)

    queryAttempt match {
      case Failure(e) => Failure(e)
      case Success(list) => Success(new S3(
        HashMap(list.map(metadata => metadata.path -> metadata): _*),
        enableMDBVersioning
      ))
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

    // if internal versioned path of a component is defined, then the blob enables MDB Versioning
    val enableMDBVersioning = componentList.head.path.get.internal_versioned_path.isDefined
    new S3(HashMap(metadataList: _*), enableMDBVersioning)
  }

  /** Combine two S3 instances
   *  @param firstBlob: first S3 blob
   *  @param secondBlob: second S3 blob
   *  @return failure if the two blobs have conflicting entries; the combined blob otherwise.
   */
  def reduce(firstBlob: S3, secondBlob: S3): Try[S3] = {
    if (firstBlob.enableMDBVersioning ^ secondBlob.enableMDBVersioning)
      Failure(new IllegalArgumentException("Cannot combine a blob that enables versioning with a blob that does not"))
    else if (firstBlob.notConflicts(secondBlob))
      Success(new S3(firstBlob.contents ++ secondBlob.contents, firstBlob.enableMDBVersioning))
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
