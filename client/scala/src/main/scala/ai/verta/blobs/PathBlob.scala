package ai.verta.blobs

import ai.verta.swagger._public.modeldb.versioning.model._

import java.security.{MessageDigest, DigestInputStream}
import java.io.{File, FileInputStream}

import java.io.File

/** Captures metadata about files
 *  @param paths list of filepaths or directory paths
 */
case class PathBlob(val paths: List[String]) extends Dataset {
  val BufferSize = 8192

  val components = paths.map(expanduser _).flatMap((path: String) => get_path_metadata(new File(path)))

  val versioningBlob = VersioningBlob(
    dataset = Some(VersioningDatasetBlob(
      path = Some(VersioningPathDatasetBlob(Some(components)))
    ))
  )

  /** Hash the file, with a buffer of size 8192 bytes
   *  From https://stackoverflow.com/questions/41642595/scala-file-hashing
   *  @param path filepath
   *  @param algorithm MD5 or SHA-256
   */
   def hash(file: File, algorithm: String): String = {
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
     messageDigest.digest.map("%x".format(_)).mkString
   }

   /** Get the metadata of path.
    *  Handle the case where the path is a directory, in which case the method is called recursively
    *  @param file file
    *  TODO: use non-recursive DFS/BFS instead to prevent stack overflow
    */
   private def get_path_metadata(file: File): List[VersioningPathDatasetComponentBlob] = {
     if (file.isDirectory()) file.listFiles().toList.flatMap(get_path_metadata)
     else List(get_file_metadata(file))
   }

   /** Get the metadata of the file
    *  @param file file
    *  @return the metadata of the file, as required by VersioningPathDatasetComponentBlob
    */
   private def get_file_metadata(file: File) = VersioningPathDatasetComponentBlob(
       last_modified_at_source = Some(BigInt(file.lastModified())),
       md5 = Some(hash(file, "MD5")),
       path = Some(file.getPath()),
       sha256 = Some(hash(file, "SHA-256")),
       size = Some(BigInt(file.length))
     )
}
