package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.blobs._

import java.security.{MessageDigest, DigestInputStream}
import java.io.{File, FileInputStream}

import scala.collection.mutable.HashMap
import scala.util.{Failure, Success, Try}

trait Dataset extends Blob {
  protected val contents: HashMap[String, FileMetadata] // for deduplication and comparing
  protected val enableMDBVersioning: Boolean // whether to version the blob with ModelDB

  /** Helper to convert VersioningPathDatasetComponentBlob to FileMetadata
   */
  protected def toComponent(metadata: FileMetadata, internalVersionedPath: Option[String] = None) =
    VersioningPathDatasetComponentBlob(
      internal_versioned_path = internalVersionedPath,
      last_modified_at_source = Some(metadata.lastModified),
      md5 = Some(metadata.md5),
      path = Some(metadata.path),
      size = Some(metadata.size)
    )

  /** Prepare the components and data for upload.
   *  @return a map of paths (in the blob) to instances of UploadComponent
   *  which contains the path to the object in local file system and the versioning component
   */
  private[verta] def prepareForUpload(): Try[Map[String, UploadComponent]]

  /** Get the metadata of a certain file stored in the dataset blob
   *  @param path path to the file
   *  @return None if path is not in dataset blob, or some file metadata.
   */
  def getMetadata(path: String) = contents.get(path)

  /** Get all the Dataset blob's corresponding list of components */
  protected def components = getAllMetadata.map(toComponent(_)).toList

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

  /** Clean up the uploaded components */
  protected def cleanUpUploadedComponents(uploadMap: Map[String, UploadComponent]): Try[Unit] = Success(())
}

object Dataset {
  /** Helper to convert VersioningPathDatasetComponentBlob to FileMetadata
   */
   private[dataset] def toMetadata(
     component: VersioningPathDatasetComponentBlob,
     versionId: Option[String] = None
   ) = new FileMetadata(
     component.last_modified_at_source.get,
     component.md5.get,
     component.path.get,
     component.size.get,
     versionId
   )

   /** Analogous to Python's os.path.expanduser
    *  From https://stackoverflow.com/questions/6803913/java-analogous-to-python-os-path-expanduser-os-path-expandvars
    *  @param path path
    *  @return path, but with (first occurence of) ~ replace with user's home directory
    */
   private[dataset] def expanduser(path: String) = path.replaceFirst("~", System.getProperty("user.home"))

   /** Hash the file's content
    *  From https://stackoverflow.com/questions/41642595/scala-file-hashing
    *  @param path filepath
    */
   private[dataset] def hash(file: File, algorithm: String) = Try {
     val BufferSize = 1024 * 1024 // 1 MB
     val buffer = new Array[Byte](BufferSize)
     val messageDigest = MessageDigest.getInstance(algorithm)

     val dis = new DigestInputStream(
       new FileInputStream(file),
       messageDigest
     )

     try {
       while (dis.read(buffer) != -1) {}
     } finally {
       dis.close()
     }

     // Convert to hexadecimal
     messageDigest.digest.map("%02x".format(_)).mkString
   }
}
