package ai.verta.blobs.dataset

/** Represent a file's metadata stored in Dataset Blob
 *  @param lastModified last time file was modified
 *  @param md5 MD5 hash of the file
 *  @param path path of the file
 *  @param size size of the file
 *  @param versionId: (optional) versionId of the file. Only exists for S3 files
 */
case class FileMetadata(
  val lastModified: BigInt,
  val md5: String,
  val path: String,
  val size: BigInt,
  val versionId: Option[String] = None,
  private[verta] var internalVersionedPath: Option[String] = None // MDB internal versioned path
) {
  // mutable fields, which are updated when preparing for uploading:
  /** TODO: remove these mutable fields to make FileMetadata immutable again */
  private[verta] var localPath: Option[String] = None // path to file in local system

  override def equals(other: Any) = other match {
    case other: FileMetadata => lastModified == other.lastModified &&
      md5 == other.md5 && path == other.path && size == other.size &&
      ((versionId.isEmpty && other.versionId.isEmpty) ||
      (versionId.isDefined && other.versionId.isDefined && versionId.get == other.versionId.get))
    case _ => false
  }
}
