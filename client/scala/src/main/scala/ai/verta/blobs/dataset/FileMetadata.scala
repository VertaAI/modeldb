package ai.verta.blobs.dataset

/** Represent a file's metadata stored in Dataset Blob
 *  @param lastModified last time file was modified
 *  @param md5 MD5 hash of the file
 *  @param path path of the file
 *  @param size size of the file
 */
class FileMetadata(val lastModified: BigInt, val md5: String, val path: String, val size: BigInt) {
  override def equals(other: Any) = other match {
    case other: FileMetadata => lastModified == other.lastModified &&
    md5 == other.md5 && path == other.path && size == other.size
    case _ => false
  }
}
