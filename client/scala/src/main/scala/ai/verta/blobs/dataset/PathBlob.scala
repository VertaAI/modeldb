package ai.verta.blobs.dataset

import ai.verta.swagger._public.modeldb.versioning.model._

import java.security.{MessageDigest, DigestInputStream}
import java.io.{File, FileInputStream}

import scala.collection.mutable.HashMap
import scala.util.{Failure, Success, Try}

/** Captures metadata about files
 *  @param paths list of filepaths or directory paths
 */
 case class PathBlob(private val paths: List[String]) extends Dataset {
  private val BufferSize = 8192
  paths.map(expanduser _).map((path: String) => processPath(new File(path)))

  override def equals(other: Any) = other match {
    case other: PathBlob => contents.equals(other.contents)
    case _ => false
  }

  /** Hash the file's content
   *  From https://stackoverflow.com/questions/41642595/scala-file-hashing
   *  @param path filepath
   */
  private def hash(file: File): String = {
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
   private def processPath(file: File) = {
     var files = List(file) // stack

     while (files.length > 0) {
       var fileToProcess = files.head
       files = files.drop(1)

       if (fileToProcess.isDirectory()) {
         files = fileToProcess.listFiles().toList ::: files
       }
       else {
         processFile(fileToProcess)
       }
     }
   }

  /** Extract the metadata of the file
   *  If the file has already been processed, or if the path is invalid, return None
   *  @param file file
   *  @return the metadata of the file, wrapped in (some) VersioningPathDatasetComponentBlob
   */
   private def processFile(file: File) = {
    if (!contents.contains(file.getPath())) {
      Try {
        new FileMetadata(
          BigInt(file.lastModified()),
          hash(file),
          file.getPath(),
          BigInt(file.length)
        )
      } match {
        case Success(metadata) => {
          contents.put(file.getPath(), metadata)
          Success(())
        }
        case Failure(e) => Failure(e)
      }
    }
    else Failure(new IllegalArgumentException("File has already been added."))
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

    pathVersioningBlob.components.get.map(
      comp => pathBlob.contents.put(comp.path.get, pathBlob.toMetadata(comp))
    )
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
