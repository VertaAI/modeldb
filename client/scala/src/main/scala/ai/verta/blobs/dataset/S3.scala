package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._
import java.net.{URL, URI}

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._

import collection.JavaConverters._

import scala.collection.mutable.HashMap
import scala.util.{Failure, Success, Try}

/** Captures metadata about S3 objects
 *  To create a new instance, use the constructor taking a list of S3 Locations
 *  {{{
 *  val s3Blob: Try[S3] = S3(List(new S3Location("some-path-1"), new S3Location("some-path-2")))
 *  }}}
 */
case class S3(
  private val metadataList: List[Tuple2[String, FileMetadata]],
  private val versionList: List[Tuple2[String, String]]
) extends Dataset {
  protected var contents = HashMap(metadataList: _*)
  private var versionMap = HashMap(versionList: _*)

  /** Get the version id of a file
   *  @param path: S3 URL of a file in the form "s3://<bucketName>/<key>"
   *  @return the version id of the file
   */
  def getVersionId(path: String) = versionMap.get(path)

  override def equals(other: Any) = other match {
    case other: S3 => contents.equals(other.contents) && versionMap.equals(other.versionMap)
    case _ => false
  }
}

/** Companion object to handle interaction with versioning blob */
object S3 {
  private val s3: AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION).build()

  /** Constructor that user should use:
   */
  def apply(paths: List[S3Location]): Try[S3] = {
    val queryAttempt = Try(
      paths.map(getS3LocMetadata).map(_.get)
    ).map(_.flatten)

    queryAttempt match {
      case Failure(e) => Failure(e)
      case Success(list) => Success(new S3(
        list.map(pair => pair._1.path -> pair._1),
        list.filter(_._2.isDefined).map(pair => pair._1.path -> pair._2.get)
      ))
    }
  }

  /** Factory method to convert a versioning blob instance
   *  @param s3VersioningBlob the versioning blob to convert
   */
  def apply(s3VersioningBlob: VersioningS3DatasetBlob) {
    var s3Blob = new S3(List(), List())
    val componentList = s3VersioningBlob.components.get
    val metadataList = componentList.map(
      comp => comp.path.get.path.get -> s3Blob.toMetadata(comp.path.get)
    )
    val versionList = componentList.filter(_.s3_version_id.isDefined).map(
      comp => comp.path.get.path.get -> comp.s3_version_id.get
    )

    s3Blob.contents = HashMap(metadataList: _*)
    s3Blob.versionMap = HashMap(versionList: _*)

    s3Blob
  }

  /** Convert a S3 instance to a VersioningBlob
   *  @param blob S3 instance
   *  @return equivalent VersioningBlob instance
   */
  def toVersioningBlob(blob: S3) = {
    val s3Components = blob.components.map(comp => VersioningS3DatasetComponentBlob(
      Some(comp),
      blob.versionMap.get(comp.path.get)
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
    if (loc.key.isEmpty || loc.key.get.charAt(loc.key.get.length - 1) == '/') {
      // no key, or is a folder
      // null or empty string?
      val versionListing =
        if (loc.key.isEmpty) s3.listVersions(loc.bucketName, null)
        else s3.listVersions(loc.bucketName, loc.key.get)

      handleVersionListing(versionListing, List())
    }
    else {
      val request =
        if (loc.versionID.isDefined) new GetObjectMetadataRequest(loc.bucketName, loc.key.get, loc.versionID.get)
        else new GetObjectMetadataRequest(loc.bucketName, loc.key.get)

      getObjectMetadata(s3.getObjectMetadata(request), loc.bucketName, loc.key.get).map(List(_))
    }
  }

  /** Helper function to deal with VersionListing when key is not provided
   */
  private def handleVersionListing(
    versionListing: VersionListing,
    acc: List[Tuple2[FileMetadata, Option[String]]]
  ): Try[List[Tuple2[FileMetadata, Option[String]]]] = {
    val batchAttempt = Try(
      versionListing.getVersionSummaries().asScala.toList
                    .filter((version: S3VersionSummary) => version.getKey().charAt(version.getKey().length() - 1) != '/') // not a folder
                    .filter(_.isLatest())
                    .map(getVersionMetadata) // List[Try]
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
    val objPath = getPath(bucketName, key)
    val metadata = new FileMetadata(
      BigInt(obj.getLastModified().getTime()), // convert time to UNIX timestamp (ms)
      obj.getETag(),
      objPath,
      BigInt(obj.getContentLength())
    )

    if (obj.getVersionId() != null)
      new Tuple2(metadata, Some(obj.getVersionId()))
    else
      new Tuple2(metadata, None)
  }

  /** Helper function to extract metadata from a version summary
   */
  private def getVersionMetadata(version: S3VersionSummary) = Try {
    val versionPath = getPath(version.getBucketName(), version.getKey())
    val metadata = new FileMetadata(
      BigInt(version.getLastModified().getTime()),
      version.getETag(),
      versionPath,
      BigInt(version.getSize())
    )

    if (version.getVersionId() != null)
      new Tuple2(metadata, Some(version.getVersionId()))
    else
      new Tuple2(metadata, None)
  }

  /** Helper function to construct path from bucket name and key */
  private def getPath(bucketName: String, key: String) = f"s3://${bucketName}/${key}"
}

/** A location in S3
 *  @param path S3 URL of the form "s3://<bucketName>" or "s3://<bucketName>/<key>"
 *  @param versionID: Version of the S3 file. Only relevant if path is to a file. If not set, latest version will be retrieved.
 */
class S3Location(val path: String, val versionID: Option[String] = None) {
  private val uri = new URI(path)
  if (uri.getScheme() != "s3") throw new IllegalArgumentException("Illegal path")

  val bucketName = uri.getAuthority()
  val key = obtainKey(uri.getPath())

  private def obtainKey(rawPath: String): Option[String] = {
    if (rawPath.length() == 0 || (rawPath.charAt(0) == '/' && rawPath.length() == 1)) None
    else if (rawPath.charAt(0) == '/') Some(rawPath.substring(1))
    else Some(rawPath)
  }
}
