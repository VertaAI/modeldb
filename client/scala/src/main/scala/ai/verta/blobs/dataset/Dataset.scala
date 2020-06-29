package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.blobs._
import ai.verta.repository.Commit

import java.security.{MessageDigest, DigestInputStream}
import java.io.{File, FileInputStream}
import java.nio.file.Paths

import scala.collection.mutable.HashMap
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext

trait Dataset extends Blob {
  protected val contents: HashMap[String, FileMetadata] // for deduplication and comparing
  private[verta] val enableMDBVersioning: Boolean // whether to version the blob with ModelDB

  // mutable state, populated when getting blob from commit
  /** TODO: Figure out a way to remove this */
  private[verta] var commit: Option[Commit] = None
  private[verta] var blobPath: Option[String] = None // path to the blob in the commit

  /** Downloads componentPath from this dataset if ModelDB-managed versioning was enabled
   *  Currently, only support downloading to a specific path
   *  @param componentPath Original path of the file or directory in this dataset to download
   *  @param downloadToPath Path to download to
   *  @return Whether the download attempts succeed.
   */
  def download(
    componentPath: Option[String] = None,
    downloadToPath: String
  )(implicit ec: ExecutionContext): Try[Unit] = {
    /** TODO: Make downloadToPath optional */
    /** TODO: allow for download chunk by chunk */

    if (!enableMDBVersioning)
      Failure(new IllegalStateException("This blob did not allow for versioning"))
    else if (commit.isEmpty || blobPath.isEmpty)
      Failure(new IllegalStateException(
        "This dataset cannot be used for downloads. Consider using `commit.get()` to obtain a download-capable dataset"
      ))
    else
      Try {
        determineComponentAndLocalPaths(componentPath, downloadToPath)
          .map(pair => downloadComponent(pair._1, pair._2))
          .map(_.get)
      }
  }

  /** Download a single component, to a determined local destination
   *  @param componentPath Path to the component
   *  @param downloadToPath Local path to download to
   *  @return whether the download attempt succeeds.
   */
  private def downloadComponent(
    componentPath: String,
    downloadToPath: String
  )(implicit ec: ExecutionContext): Try[Unit] = {
    val file = new File(downloadToPath)

    Try ({
      Option(file.getParentFile()).map(_.mkdirs()) // create the ancestor directories, if necessary
      file.createNewFile() // create the new file, if necessary
    })
      .flatMap(_ => commit.get.downloadComponent(blobPath.get, componentPath, file))
  }

  /** Identify components to be downloaded, along with their local destination paths.
   *  @param componentPath (Optional) path to directory or file within blob.
   *  @param downloadToPath Local path to download to
   *  @return Map of component paths to local destination paths
   */
  private def determineComponentAndLocalPaths(
    componentPath: Option[String] = None,
    downloadToPath: String
  ): Map[String, String] = {
    if (componentPath.isEmpty) {
      // download entire blob
      val downloadToPaths =
        listPaths.map(comp => joinPaths(downloadToPath, removePrefixDir(comp, "s3:")))

      listPaths.zip(downloadToPaths).toMap
    }
    else if (contents.contains(componentPath.get)) // download a component
      Map(componentPath.get -> downloadToPath)
    else {
      // download a directory
      val componentPaths = getComponentPathInside(componentPath.get)
      val downloadToPaths =
        componentPaths.map(comp => joinPaths(downloadToPath, removePrefixDir(comp, componentPath.get)))

      componentPaths.zip(downloadToPaths).toMap
    }
  }

  /** Return the set of component paths inside a directory path
   *  @param path directory path
   *  @return Set of component paths inside the directory
   */
  def getComponentPathInside(path: String): List[String] = {
    val dirPath = if(path.endsWith("/")) path else path + "/"
    listPaths.filter(_.startsWith(dirPath))
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
  private[verta] def cleanUpUploadedComponents(): Try[Unit] = Success(())

  /** Removes prefix from the beginning of path (leaving it unchanged if path does not contain prefix)
   *  @param path directory path
   *  @param prefix the prefix to removed
   *  @return the path with the prefix removed
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

  /** Joining two paths
   *  @param prefix the first path
   *  @param suffix the second path
   *  @return the joined path
   */
  private def joinPaths(prefix: String, suffix: String): String =
    Paths.get(prefix, suffix).toString

  /** Returns the paths of all components in this dataset
   *  @return Paths of all components
   */
  def listPaths: List[String] = contents.keySet.toList.sorted
}

object Dataset {
  /** Helper to convert VersioningPathDatasetComponentBlob to FileMetadata
   */
   private[dataset] def toMetadata(
     component: VersioningPathDatasetComponentBlob,
     versionId: Option[String] = None
   ) = new FileMetadata(
     component.last_modified_at_source.getOrElse(0),
     component.md5.getOrElse(""),
     component.path.getOrElse(""),
     component.size.getOrElse(0),
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
