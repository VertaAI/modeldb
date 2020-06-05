package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._

import java.security.{MessageDigest, DigestInputStream}
import java.io.{File, FileInputStream}

import scala.collection.mutable.HashMap
import scala.util.{Failure, Success, Try}
import scala.annotation.tailrec

/** Captures metadata about files
 *  @param paths list of filepaths or directory paths
 *  @throws IOException - if any path is invalid (i.e non-existent)
 *  @throws SecurityException - If a security manager exists and its SecurityManager.checkRead(java.lang.String) method denies read access to any file
 */
case class PathBlob(private val paths: List[String]) extends Dataset {
  private val BufferSize = 8192

  private val metadataList = paths.map(expanduser)
  .flatMap((path: String) => processPath(new File(path)))
  .map(metadata => metadata.path -> metadata)

  protected var contents = HashMap(metadataList: _*)

  override def equals(other: Any) = other match {
    case other: PathBlob => contents.equals(other.contents)
    case _ => false
  }


  /** Hash the file's content
   *  From https://stackoverflow.com/questions/41642595/scala-file-hashing
   *  @param path filepath
   */
  private def hash(file: File) = Try {
    val buffer = new Array[Byte](BufferSize)
    val messageDigest = MessageDigest.getInstance("MD5")

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

  /** Get the metadata of path.
   *  @param file a file object, representing the path
   *  @return a list of components of file under the path
   */
  private def processPath(file: File): List[FileMetadata] = dfs(List(file), List())

  /** Tail-recursive DFS traversal to prevent stack overflow error
   *  @param stack a stack containing the files/dirs to explore
   *  @param acc accumulator list of file metadata to return
   */
  @tailrec private def dfs(stack: List[File], acc: List[FileMetadata]): List[FileMetadata] = {
    if (stack.isEmpty) acc
    else if (stack.head.isDirectory) {
      val dir = stack.head
      dfs(dir.listFiles().toList ::: stack.drop(1), acc)
    }
    else {
      val file = stack.head
      dfs(stack.drop(1), processFile(file) :: acc)
    }
  }

  /** Extract the metadata of the file
   *  If the file has an invalid path, exception is thrown immediately, and program stops (if not caught outside)
   *  @param file file
   *  @return the metadata of the file, wrapped in a FileMetadata object (if success)
   *  @throws IOException - if the path is invalid (i.e non-existent)
   *  @throws SecurityException - If a security manager exists and its SecurityManager.checkRead(java.lang.String) method denies read access to the file
   */
  private def processFile(file: File) = hash(file) match {
    case Failure(e) => throw e
    case Success(fileHash) => new FileMetadata (
      BigInt(file.lastModified()),
      fileHash,
      file.getPath(),
      BigInt(file.length)
    )
  }

  /** Analogous to Python's os.path.expanduser
   *  From https://stackoverflow.com/questions/6803913/java-analogous-to-python-os-path-expanduser-os-path-expandvars
   *  @param path path
   *  @return path, but with (first occurence of) ~ replace with user's home directory
   */
  private def expanduser(path: String) = path.replaceFirst("~", System.getProperty("user.home"))
}

/** Companion object to handle interaction with versioning blob */
object PathBlob {
  /** Factory method to convert a versioning path dataset blob instance
   *  @param pathVersioningBlob the versioning blob to convert
   *  @return equivalent PathBlob instance
   */
  def apply(pathVersioningBlob: VersioningPathDatasetBlob) = {
    var pathBlob = new PathBlob(List())
    var metadataList = pathVersioningBlob.components.get.map(
      comp => comp.path.get -> pathBlob.toMetadata(comp)
    )

    pathBlob.contents = HashMap(metadataList: _*)
    pathBlob
  }

  /** Convert a PathBlob instance to a VersioningBlob
   *  @param blob PathBlob instance
   *  @return equivalent VersioningBlob instance
   */
  def toVersioningBlob(blob: PathBlob) = VersioningBlob(
    dataset = Some(VersioningDatasetBlob(
      path = Some(VersioningPathDatasetBlob(Some(blob.components)))
    ))
  )
}
