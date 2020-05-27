package ai.verta.blobs

import ai.verta.swagger._public.modeldb.versioning.model._
import java.net.{URL, URI}

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._

import collection.JavaConverters._

/** Captures metadata about S3 objects
 */
case class S3(val paths: List[S3Location]) extends Dataset {
  val s3: AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION).build();

  val components = paths.flatMap(getS3LocMetadata _)

  val versioningBlob = VersioningBlob(
    dataset = Some(VersioningDatasetBlob(
      s3 = Some(VersioningS3DatasetBlob(Some(components)))
    ))
  )

  // s3.close()

  /** Helper function to query metadata of S3 Location
   */
  private def getS3LocMetadata(loc: S3Location): List[VersioningS3DatasetComponentBlob] = {
    if (loc.key.isEmpty || loc.key.get.charAt(loc.key.get.length - 1) == '/') {
      // no key, or is a folder
      // null or empty string?
      val versionListing = if (loc.key.isEmpty) s3.listVersions(loc.bucketName, null) else s3.listVersions(loc.bucketName, loc.key.get)
      handleVersionListing(versionListing)
    }
    else {
      return List(getObjectMetadata(s3.getObjectMetadata(loc.bucketName, loc.key.get), loc.bucketName, loc.key.get))
    }
  }

  /** Helper function to deal with VersionListing when key is not provided
   */
  private def handleVersionListing(versionListing: VersionListing): List[VersioningS3DatasetComponentBlob] = {
    val batch = versionListing.getVersionSummaries().asScala.toList
    .filter((version: S3VersionSummary) => version.getKey().charAt(version.getKey().length() - 1) != '/') // not a folder
    .filter(_.isLatest())
    .map(getVersionMetadata _)

    if (versionListing.isTruncated()) handleVersionListing(s3.listNextBatchOfVersions(versionListing)) ::: batch
    else batch
  }


  /** Helper function to extract metadata from a version summary and store in a component blob
   *  TODO: convert time to proper format
   */
  private def getVersionMetadata(version: S3VersionSummary): VersioningS3DatasetComponentBlob = {
    val md5 = version.getETag()

    VersioningS3DatasetComponentBlob(
      Some(VersioningPathDatasetComponentBlob(
        md5 = Some(md5.substring(1, md5.length() - 1)), // trim the opening and closing quotes
        path = Some(getPath(version.getBucketName(), version.getKey())),
        // last_modified_at_source = Some(BigInt(version.getLastModified())),
        size = Some(version.getSize())
      )),
      if (version.getVersionId() == null) None else Some(version.getVersionId())
    )
  }


  /** Helper function to extract metadata from the return object
   *  TODO: convert time to proper format
   */
  private def getObjectMetadata(obj: ObjectMetadata, bucketName: String, key: String) = {
    val md5 = obj.getETag()

    VersioningS3DatasetComponentBlob(
      Some(VersioningPathDatasetComponentBlob(
        md5 = Some(md5.substring(1, md5.length() - 1)), // trim the opening and closing quotes
        path = Some(getPath(bucketName, key)),
        // last_modified_at_source = Some(BigInt(version.getLastModified())),
        size = Some(obj.getContentLength())
      )),
      if (obj.getVersionId() == null) None else Some(obj.getVersionId())
    )
  }

  /** Helper function to construct path from bucket name and key */
  private def getPath(bucketName: String, key: String) = f"s3://${bucketName}/${key}"
}


/** A location in S3
 *  @param url S3 URL of the form "s3://<bucketName>" or "s3://<bucketName>/<key>"
 */
class S3Location(val path: String, val versionID: Option[String] = None) {
  val uri = new URI(path)
  if (uri.getScheme() != "s3") throw new IllegalArgumentException("Illegal path")

  val bucketName = uri.getAuthority()
  val key = obtainKey(uri.getPath())

  private def obtainKey(rawPath: String): Option[String] = {
    if (rawPath.length() == 0 || (rawPath.charAt(0) == '/' && rawPath.length() == 1)) None
    else if (rawPath.charAt(0) == '/') Some(rawPath.substring(1))
    else Some(rawPath)
  }
}
