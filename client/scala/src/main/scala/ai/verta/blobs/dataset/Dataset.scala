package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._

import scala.collection.mutable.HashMap

trait Dataset extends Blob {
  protected var contents = new HashMap[String, FileMetadata]() // for deduplication and comparing

  /** Helper to convert VersioningPathDatasetComponentBlob to FileMetadata
   */
  protected def toMetadata(component: VersioningPathDatasetComponentBlob) = new FileMetadata(
    component.last_modified_at_source.get,
    component.md5.get,
    component.path.get,
    component.size.get
  )

  /** Helper to convert VersioningPathDatasetComponentBlob to FileMetadata
   */
  protected def toComponent(metadata: FileMetadata) = VersioningPathDatasetComponentBlob(
    last_modified_at_source = Some(metadata.lastModified),
    md5 = Some(metadata.md5),
    path = Some(metadata.path),
    size = Some(metadata.size)
  )

  /** Get the metadata of a certain file stored in the dataset blob
   *  @param path path to the file
   *  @return None if path is not in dataset blob, or some file metadata.
   */
  def getMetadata(path: String): Option[FileMetadata] = contents.get(path)

  /** Get all the files' metadata managed by the Dataset blob */
  protected def components = contents.values.map(toComponent _).toList
}

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
