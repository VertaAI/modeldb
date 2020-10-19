package ai.verta.blobs.dataset

import java.net.{URL, URI}

import scala.util.{Try, Success, Failure}

/** A location in S3.
 *  To initialize, pass in an S3 path and (optionally) a version ID:
 *  {{{
 *  val S3Location: Try[S3Location] = S3Location("some-path-1")
 *  }}}
 */
class S3Location(
  val bucketName: String,
  val key: Option[String] = None,
  val versionID: Option[String] = None
) {}

object S3Location {
  /** Factory method to create S3 location.
   *  @param path S3 URL of the form "s3://<bucketName>" or "s3://<bucketName>/<key>"
   *  @param versionID: Version of the S3 file. Only relevant if path is to a file. If not set, latest version will be retrieved.
   *  @return if a non-S3 path is passed, a Failure with IllegalArgumentException; otherwise S3Location wrapped in Success.
   */
  def apply(path: String, versionID: Option[String] = None) = Try {
    val uri = new URI(path)

    if (uri.getScheme() != "s3")
      throw new IllegalArgumentException("Illegal path; must be an S3 location")
    else
      new S3Location(uri.getAuthority(), obtainKey(uri.getPath()), versionID)
  }

  private def obtainKey(rawPath: String): Option[String] = {
    if (rawPath.length() == 0 || (rawPath.charAt(0) == '/' && rawPath.length() == 1)) None
    else if (rawPath.charAt(0) == '/') Some(rawPath.substring(1))
    else Some(rawPath)
  }
}
