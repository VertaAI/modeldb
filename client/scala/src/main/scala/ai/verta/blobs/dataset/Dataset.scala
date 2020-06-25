package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.blobs._
import ai.verta.repository.Commit

import java.security.{MessageDigest, DigestInputStream}
import java.io.{File, FileInputStream}

import scala.collection.mutable.HashMap
import scala.util.{Failure, Success, Try}

trait Dataset extends Blob {
  protected val contents: HashMap[String, FileMetadata] // for deduplication and comparing
  private[verta] val enableMDBVersioning: Boolean // whether to version the blob with ModelDB
  private val ChunkSize: Int = 32 * 1024 * 1024 // default chunk size, 32 MB

  // mutable state, populated when getting blob from commit
  /** TODO: Figure out a way to remove this */
  private[verta] var commit: Option[Commit] = None
  private[verta] var blobPath: Option[String] = None // path to the blob in the commit

  /** Downloads componentPath from this dataset if ModelDB-managed versioning was enabled
   *  @param componentPath Original path of the file or directory in this dataset to download
   *  @param downloadToPath Path to download to
   *  @param chunkSize Number of bytes to download at a time (default: 32 MB)
   *  @return Whether the download attempts succeed.
   */
  def download(
    componentPath: String,
    downloadToPath: String,
    chunkSize: Int = ChunkSize
  ): Try[Unit] = {
    if (!enableMDBVersioning)
      Failure(new IllegalStateException("This blob did not allow for versioning"))
    else if (commit.isEmpty || blobPath.isDefined)
      Failure(new IllegalStateException(
        "This dataset cannot be used for downloads. Consider using `commit.get()` to obtain a download-capable dataset"
      ))
    else
      Try {
        val file = new File(downloadToPath)
        file.mkdirs() // create the ancestor directories, if necessary
        file.createNewFile() // create the new file, if necessary

        val url = commit.getUrlForArtifact(blobPath.get, datasetComponentPath.get, "GET").get
        commit.downloadFromURL(url, file)
      }
  }

  /** Helper to convert VersioningPathDatasetComponentBlob to FileMetadata
   */
  protected def toComponent(metadata: FileMetadata) =
    VersioningPathDatasetComponentBlob(
      internal_versioned_path = metadata.internalVersionedPath,
      last_modified_at_source = Some(metadata.lastModified),
      md5 = Some(metadata.md5),
      path = Some(metadata.path),
      size = Some(metadata.size)
    )

  /** Prepare the components and data for upload.
   *  @return whether the attempt succeeds.
   */
  private[verta] def prepareForUpload(): Try[Unit]

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

  /** Clean up the uploaded components */
  protected def cleanUpUploadedComponents(): Try[Unit] = Success(())
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
