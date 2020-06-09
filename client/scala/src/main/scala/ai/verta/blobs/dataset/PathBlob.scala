package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._

import java.security.{MessageDigest, DigestInputStream}
import java.io.{File, FileInputStream}

import scala.collection.mutable.HashMap
import scala.util.{Failure, Success, Try}
import scala.annotation.tailrec

/** Captures metadata about files
 *  To create a new instance, use the constructor taking a list of paths (each is a string) or a single path:
 *  {{{
 *  val pathList = List("some-path1", "some-path2")
 *  val pathBlob: Try[PathBlob] = PathBlob(pathList)
 *  val pathBlob2: Try[PathBlob] = PathBlob("some-other-path")
 *  }}}
 *  If an invalid path is passed to the constructor, it will return a failure.
 */
case class PathBlob(protected val contents: HashMap[String, FileMetadata]) extends Dataset {
  override def equals(other: Any) = other match {
    case other: PathBlob => contents.equals(other.contents)
    case _ => false
  }
}

/** Companion object to initialize instances and handle interaction with versioning blob */
object PathBlob {
  private val BufferSize = 8192

  /** Constructor taking only one path
   *  @param path a single path
   *  @return if the path is invalid, a failure along with exception message. Otherwise, the pathblob (wrapped in success)
   */
  def apply(path: String): Try[PathBlob] = apply(List(path))

  /** The constructor that user should use to create a new instance of PathBlob.
   *  @return if any path is invalid, a failure along with exception message. Otherwise, the pathblob (wrapped in success)
   */
  def apply(paths: List[String]): Try[PathBlob] = {
    val metadataLists = Try(
      paths.map(expanduser)
           .map((path: String) => processPath(new File(path)))
           .map(_.get)
    )

    metadataLists match {
      case Failure(e) => Failure(e)
      case Success(list) => Success(new PathBlob(
        HashMap(list.flatten.map(metadata => metadata.path -> metadata): _*)
      ))
    }
  }

  /** Factory method to combine two PathBlob instances
   *  @param firstBlob: first pathblob
   *  @param secondBlob: second pathblob
   *  @return failure if the two blobs have conflicting entries; the combined blob otherwise.
   */
  def reduce(firstBlob: PathBlob, secondBlob: PathBlob): Try[PathBlob] = {
    if (firstBlob.notConflicts(secondBlob))
      Success(new PathBlob(firstBlob.contents ++ secondBlob.contents))
    else
      Failure(new IllegalArgumentException("The two blobs have conflicting entries"))
  }

  /** Factory method to convert a versioning path dataset blob instance. Not meant to be used by user
   *  @param pathVersioningBlob the versioning blob to convert
   *  @return equivalent PathBlob instance
   */
  def apply(pathVersioningBlob: VersioningPathDatasetBlob) = {
    val metadataList = pathVersioningBlob.components.get.map(
      comp => comp.path.get -> Dataset.toMetadata(comp)
    )
    new PathBlob(HashMap(metadataList: _*))
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
  private def processPath(file: File): Try[List[FileMetadata]] = dfs(List(file), List())

  /** Tail-recursive DFS traversal to prevent stack overflow error
   *  @param stack a stack containing the files/dirs to explore
   *  @param acc accumulator list of file metadata to return
   */
  @tailrec private def dfs(stack: List[File], acc: List[FileMetadata]): Try[List[FileMetadata]] = {
    if (stack.isEmpty) Success(acc)
    else if (stack.head.isDirectory) {
      val dir = stack.head
      dfs(dir.listFiles().toList ::: stack.tail, acc)
    }
    else {
      val metadata = processFile(stack.head)

      metadata match {
        case Failure(e) => Failure(e)
        case Success(m) => dfs(stack.tail, m :: acc)
      }
    }
  }

  /** Extract the metadata of the file
   *  If the file has an invalid path, the exception (IOException or security exception) is wrapped in Failure and returned
   *  @param file file
   *  @return the metadata of the file, wrapped in a FileMetadata object (if success)
   */
  private def processFile(file: File) = hash(file) match {
    case Failure(e) => Failure(e)
    case Success(fileHash) => Try(new FileMetadata (
      BigInt(file.lastModified()),
      fileHash,
      file.getPath(),
      BigInt(file.length)
    ))
  }

  /** Analogous to Python's os.path.expanduser
   *  From https://stackoverflow.com/questions/6803913/java-analogous-to-python-os-path-expanduser-os-path-expandvars
   *  @param path path
   *  @return path, but with (first occurence of) ~ replace with user's home directory
   */
  private def expanduser(path: String) = path.replaceFirst("~", System.getProperty("user.home"))
}
