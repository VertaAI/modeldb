package ai.verta.blobs

import ai.verta.swagger._public.modeldb.versioning.model._
import java.net.{URL, URI}

/** Captures metadata about S3 objects
 *  TODO: implement S3.location()
 */
// case class S3(val paths: List[String]) extends Dataset {
//
// }

/** A location in S3
 *  @param url S3 URL of the form "s3://<bucketName>" or "s3://<bucketName>/<key>"
 */
class S3Location(val path: String, val versionID: Option[String] = None) {
  val uri = new URI(path)
  if (uri.getScheme() != "s3") throw new IllegalArgumentException("Illegal path")

  val bucketName = uri.getAuthority()
  val key = obtainKey(uri.getPath())

  private def obtainKey(rawPath: String): Option[String] = {
    if (rawPath.length() == 0) None
    else if (rawPath.charAt(0) == '/') obtainKey(rawPath.substring(1))
    else Some(rawPath)
  }
}
