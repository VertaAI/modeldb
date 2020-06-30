package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._
import ai.verta.blobs._

import java.security.{MessageDigest, DigestInputStream}
import java.io.{File, FileInputStream}
import java.nio.file.{Path, Paths, Files}

import scala.collection.mutable.HashMap
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext

trait Dataset extends Blob {
  protected val contents: HashMap[String, FileMetadata] // for deduplication and comparing
  private[verta] val enableMDBVersioning: Boolean // whether to version the blob with ModelDB

  // mutable state, populated when getting blob from commit
  /** TODO: Figure out a way to remove this */
  // Function to downwload a component, given its path in the blob, the blob's path in the commit
  // and the file pointing to local path to download to:
  private[verta] var downloadFunction: Option[(String, String, File) => Try[Unit]] = None
  private[verta] var blobPath: Option[String] = None // path to the blob in the commit

  /** Downloads componentPath from this dataset if ModelDB-managed versioning was enabled
   *  @param componentPath Original path of the file or directory in this dataset to download.
   *  If not provided, all files will be downloaded
   *  @param downloadToPath Path to download to. If not provided, the file(s) will be downloaded into a new path in
   *  the current directory. If provided and the path already exists, it will be overwritten
   *  @return If succeeds, absolute path where file(s) were downloaded to. Matches downloadToPath if provided.
   */
  def download(
    componentPath: Option[String] = None,
    downloadToPath: Option[String] = None
  )(implicit ec: ExecutionContext): Try[String] = {
    if (!enableMDBVersioning)
      Failure(new IllegalStateException("This blob did not allow for versioning"))
    else if (downloadFunction.isEmpty || blobPath.isEmpty)
      Failure(new IllegalStateException(
        "This dataset cannot be used for downloads. Consider using `commit.get()` to obtain a download-capable dataset"
      ))
    else {
      val componentToLocalPath = determineComponentAndLocalPaths(componentPath, downloadToPath)

      Try ({
        componentToLocalPath.componentToLocalPath
          .map(pair => downloadComponent(pair._1, pair._2))
          .map(_.get)
      }) match {
        case Success(_) => Success(componentToLocalPath.absoluteLocalPath)
        case Failure(e) => {
          componentToLocalPath.componentToLocalPath.values.map(path => Try((new File(path)).delete()))
          Failure(e)
        }
      }
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
      .flatMap(_ => downloadFunction.get(blobPath.get, componentPath, file))
  }

  /** Identify components to be downloaded, along with their local destination paths.
   *  @param componentPath (Optional) path to directory or file within blob.
   *  @param downloadToPath Local path to download to
   *  @return Map of component paths to local destination paths,
   *  along with absolute local path to the downloaded file(s)
   */
  private def determineComponentAndLocalPaths(
    componentPath: Option[String] = None,
    downloadToPath: Option[String] = None
  ): ComponentToLocalPath = {
    val safeDownloadToPath = determineDownloadToPath(componentPath, downloadToPath)

    val componentToLocalPath =
      if (componentPath.isEmpty) {
        // download entire blob
        val downloadToPaths =
          listPaths.map(comp => joinPaths(safeDownloadToPath, removePrefixDir(comp, "s3:")))

        listPaths.zip(downloadToPaths).toMap
      }
      else if (contents.contains(componentPath.get)) // download a component
        Map(componentPath.get -> safeDownloadToPath)
      else {
        // download a directory
        val componentPaths = getComponentPathInside(componentPath.get)
        val downloadToPaths =
          componentPaths.map(comp => joinPaths(safeDownloadToPath, removePrefixDir(comp, componentPath.get)))

        componentPaths.zip(downloadToPaths).toMap
      }

    ComponentToLocalPath(componentToLocalPath, getAbsolutePath(safeDownloadToPath))
  }

  /** Determine a safe local path to download to.
   *  If the user explicitly passes a downloadToPath, it will be used
   *  Otherwise, it will be determined as follows:
   *
   *  1. If componentPath is defined and does not refer to current directory, use it
   *
   *  2. Else, use the default path, which is "mdb-data-download"
   *
   *  If the download-to-path has to be inferred, then it is incremented until collision is avoided
   *  (i.e no such file/directory exists in that path)
   */
  private def determineDownloadToPath(
    componentPath: Option[String] = None,
    downloadToPath: Option[String] = None
  ): String = downloadToPath.getOrElse({
    val originalPath =
      if (componentPath.isEmpty)
        Dataset.DefaultDownloadDir
      else {
        val componentPathName = (new File(componentPath.get)).getName

        if (Set(".", "..", "/", "s3:").contains(componentPathName))
          Dataset.DefaultDownloadDir // rather than dump everything into current directory
        else
          componentPathName
      }

    avoidCollision(originalPath)
  })

  /** Increments the original path until collision is avoided
   *  @param path original path
   *  @return the first incremented path which does not exist in local file system
   */
  private def avoidCollision(path: String): String = {
    val components = separateExtension(path)
    val base = components(0)
    val extension = components(1)

    var file: Path = Paths.get(path)
    var inc = 1

    while (Files.exists(file)) {
      file = Paths.get(f"${base} ${inc}${extension}")
      inc += 1
    }

    file.toString()
  }

  /** Separate the extension from the base of path
   *  @param path path
   *  @return an array, where first entry is base, and second entry is extension
   */
  private def separateExtension(path: String) = {
    val components = new Array[String](2)
    val delimiterIndex = path.lastIndexOf(".")

    components(0) = if (delimiterIndex == -1) path else path.substring(0, delimiterIndex)
    components(1) = if (delimiterIndex == -1) "" else path.substring(delimiterIndex)

    components
  }

  /** Return the list of component paths inside a directory path
   *  @param path directory path
   *  @return Set of component paths inside the directory
   */
  private def getComponentPathInside(path: String): List[String] = {
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

  private def getAbsolutePath(path: String): String =
    (new File(path)).getAbsolutePath()

  /** Returns the paths of all components in this dataset
   *  @return Paths of all components
   */
  def listPaths: List[String] = contents.keySet.toList.sorted
}

object Dataset {
  /** Default download directory */
  val DefaultDownloadDir: String = "mdb-data-download"

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
