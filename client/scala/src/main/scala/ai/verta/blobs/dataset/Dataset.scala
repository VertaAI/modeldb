package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.blobs._
import ai.verta.repository.Commit

import java.security.{MessageDigest, DigestInputStream}
import java.io.{File, FileInputStream}

import scala.collection.mutable.HashMap
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext

trait Dataset extends Blob {
  protected val contents: HashMap[String, FileMetadata] // for deduplication and comparing
  private[verta] val enableMDBVersioning: Boolean // whether to version the blob with ModelDB
  private val ChunkSize: Int = 32 * 1024 * 1024 // default chunk size, 32 MB

  // mutable state, populated when getting blob from commit
  /** TODO: Figure out a way to remove this */
  private[verta] var commit: Option[Commit] = None
  private[verta] var blobPath: Option[String] = None // path to the blob in the commit

  /** Downloads componentPath from this dataset if ModelDB-managed versioning was enabled
   *  Currently, only support downloading a specific component/folder to a specific path
   *  @param componentPath Original path of the file or directory in this dataset to download
   *  @param downloadToPath Path to download to
   *  @param chunkSize Number of bytes to download at a time (default: 32 MB)
   *  @return Whether the download attempts succeed.
   */
  def download(
    componentPath: String,
    downloadToPath: String,
    chunkSize: Int = ChunkSize
  )(implicit ec: ExecutionContext): Try[Unit] = {
    if (!enableMDBVersioning)
      Failure(new IllegalStateException("This blob did not allow for versioning"))
    else if (commit.isEmpty || blobPath.isEmpty)
      Failure(new IllegalStateException(
        "This dataset cannot be used for downloads. Consider using `commit.get()` to obtain a download-capable dataset"
      ))
    else if (contents.contains(componentPath)) {
      val file = new File(downloadToPath)

      for (
        _ <- Try(file.mkdirs()); // create the ancestor directories, if necessary
        _ <- Try(file.createNewFile()); // create the new file, if necessary
        url <- commit.get.getURLForArtifact(blobPath.get, componentPath, "GET")
      ) yield commit.get.downloadFromURL(url, file)
    }
    else  // is a directory
      Try {
        getComponentPathInside(componentPath)
          .map(comp => download(comp, f"${downloadToPath}/${removePrefixDir(comp, componentPath)}").get)
      }
  }

  /** Return the set of component paths inside a directory path
   *  @param path directory path
   *  @return Set of component paths inside the directory
   */
  def getComponentPathInside(path: String): Iterable[String] = {
    val dirPath = if(path.endsWith("/")) path else path + "/"
    contents.keySet.filter(_.startsWith(dirPath))
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

  /** Removes prefix from the beginning of path (leaving it unchanged if path does not contain prefix)
   *  @param path directory path
   *  @param prefix
   */
  private def removePrefixDir(path: String, prefix: String) = {
    val prefixDirPath = if (prefix.endsWith("/")) prefix else prefix + "/"

    if (path.startsWith(prefixDirPath + "/"))
      path.substring(prefixDirPath.length + 1)
    else if (path.startsWith(prefixDirPath))
      path.substring(prefixDirPath.length)
    else
      path
  }
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
