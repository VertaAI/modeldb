package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.blobs._

import scala.collection.mutable.HashMap
import scala.util.{Failure, Success, Try}

trait Dataset extends Blob {
  protected var contents: HashMap[String, FileMetadata] // for deduplication and comparing

  /** Helper to convert VersioningPathDatasetComponentBlob to FileMetadata
   */
  protected def toMetadata(
    component: VersioningPathDatasetComponentBlob,
    versionId: Option[String] = None
  ) = new FileMetadata(
    component.last_modified_at_source.get,
    component.md5.get,
    component.path.get,
    component.size.get,
    versionId
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
  def getMetadata(path: String) = contents.get(path)

  /** Get all the Dataset blob's corresponding list of components */
  protected def components = getAllMetadata.map(toComponent).toList

  /** Get the set of all the files' metadata managed by the Dataset blob  */
  def getAllMetadata = contents.values

  /** Check if the other dataset is combinable (i.e no conflicting entries)
   *  @param other other dataset to combine
   *  @return whether there is a conflict in the contents of two dataset
   */
  protected def notConflicts(other: Dataset) = {
    val shared = contents.keySet.intersect(other.contents.keySet)
    contents.filterKeys(shared).equals(other.contents.filterKeys(shared))
  }
}
