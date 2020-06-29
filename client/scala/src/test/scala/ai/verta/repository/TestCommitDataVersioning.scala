package ai.verta.repository

import ai.verta.client._
import ai.verta.blobs._
import ai.verta.blobs.dataset._

import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import scala.util.{Try, Success, Failure}

import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Path}
import java.util.{Arrays, Random}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestCommitDataVersioning extends FunSuite {
  implicit val ec = ExecutionContext.global

  def fixture =
    new {
        val client = new Client(ClientConnection.fromEnvironment())
        val repo = client.getOrCreateRepository("My Repo").get

        val pathBlob = PathBlob("./src/test/scala/ai/verta/blobs/testdir", true).get
        val s3Blob = S3(S3Location("s3://verta-scala-test/testdir/").get, true).get

        val commit = repo.getCommitByBranch()
          .flatMap(_.update("abc", s3Blob))
          .flatMap(_.update("def", pathBlob))
          .flatMap(_.save("some-msg")).get
    }

  def cleanup(
    f: AnyRef{val client: Client; val repo: Repository; val commit: Commit; val pathBlob: PathBlob; val s3Blob: S3}
  ) = {
    deleteDirectory(new File("./somefiles"))
    deleteDirectory(new File("./somefiles2"))

    (new File("./somefile")).delete()
    (new File("./somefile2")).delete()

    f.client.deleteRepository(f.repo.id)
    f.client.close()
  }

  /** Delete the directory */
  def deleteDirectory(dir: File): Unit = {
    Option(dir.listFiles()).map(_.foreach(deleteDirectory))
    dir.delete()
  }

  /** Check to see if two files have the same content */
  def checkEqualFile(firstFile: File, secondFile: File) = {
    val first: Array[Byte] = Files.readAllBytes(firstFile.toPath)
    val second = Files.readAllBytes(secondFile.toPath)
    Arrays.equals(first, second)
  }

  /** Generate a random file to the given path */
  def generateRandomFile(path: String, size: Int = 1024 * 1024): Try[Array[Byte]] = {
    val random = new Random()
    val contents = new Array[Byte](size)
    random.nextBytes(contents)

    val file = new File(path)
    var fileStream: Option[FileOutputStream] = None

    try {
      Try({
        Option(file.getParentFile()).map(_.mkdirs()) // create the ancestor directories, if necessary
        file.createNewFile()

        fileStream = Some(new FileOutputStream(file, false)) // overwrite, if already exists
        fileStream.get.write(contents)
      }).map(_ => contents)
    } finally {
      if (fileStream.isDefined)
        fileStream.get.close()
    }
  }

  test("downloading an uploaded file should not corrupt it") {
    val f = fixture

    try {
      // check for s3:
      val versionedS3Blob: S3 = f.commit.get("abc").get match {
        case s3: S3 => s3
      }
      val downloadS3Attempt = versionedS3Blob.download(
        Some("s3://verta-scala-test/testdir/testsubdir/testfile2"),
        "./somefile2"
      )
      assert(downloadS3Attempt.isSuccess)
      val downloadedS3MD5 = PathBlob("./somefile2").get
      assert(
        downloadedS3MD5.getMetadata("./somefile2").get.md5 equals
        f.s3Blob.getMetadata("s3://verta-scala-test/testdir/testsubdir/testfile2").get.md5
      )

      // check for path:
      val versionedPathBlob: PathBlob = f.commit.get("def").get match {
        case path: PathBlob => path
      }
      val downloadPathAttempt = versionedPathBlob.download(
        Some("./src/test/scala/ai/verta/blobs/testdir/testfile"),
        "./somefile"
      )
      assert(downloadPathAttempt.isSuccess)
      assert(checkEqualFile(
        new File("./src/test/scala/ai/verta/blobs/testdir/testfile"),
        new File("./somefile")
      ))
    } finally {
      cleanup(f)
    }
  }

  test("downloading an entire folder should retrieve all the files in the folder") {
    val f = fixture

    try {
      val versionedS3Blob: S3 = f.commit.get("abc").get match {
        case s3: S3 => s3
      }
      val downloadS3Attempt = versionedS3Blob.download(
        Some("s3://verta-scala-test/testdir/testsubdir"),
        "./somefiles2"
      )
      val downloadedS3MD5 = PathBlob("./somefiles2").get
      assert(
        downloadedS3MD5.getMetadata("./somefiles2/testfile2").get.md5 equals
        f.s3Blob.getMetadata("s3://verta-scala-test/testdir/testsubdir/testfile2").get.md5
      )

      val versionedPathBlob: PathBlob = f.commit.get("def").get match {
        case path: PathBlob => path
      }

      versionedPathBlob.download(
        Some("./src/test/scala/ai/verta/blobs/testdir"),
        "./somefiles"
      )
      assert(checkEqualFile(
        new File("./src/test/scala/ai/verta/blobs/testdir/testfile"),
        new File("./somefiles/testfile")
      ))

      assert(checkEqualFile(
        new File("./src/test/scala/ai/verta/blobs/testdir/testsubdir/testfile2"),
        new File("./somefiles/testsubdir/testfile2")
      ))
    } finally {
      cleanup(f)
    }
  }

  test("download entire blobs should retrieve all the components") {
    val f = fixture

    try {
        val versionedS3Blob: S3 = f.commit.get("abc").get match {
          case s3: S3 => s3
        }
        versionedS3Blob.download(downloadToPath = "./somefiles2")
        val downloadedS3MD5 = PathBlob("./somefiles2").get
        assert(
          downloadedS3MD5.getMetadata("./somefiles2/verta-scala-test/testdir/testsubdir/testfile2").get.md5 equals
          f.s3Blob.getMetadata("s3://verta-scala-test/testdir/testsubdir/testfile2").get.md5
        )

        val versionedPathBlob: PathBlob = f.commit.get("def").get match {
          case path: PathBlob => path
        }
        versionedPathBlob.download(downloadToPath = "./somefiles")
        assert(checkEqualFile(
          new File("./src/test/scala/ai/verta/blobs/testdir/testfile"),
          new File("./somefiles/src/test/scala/ai/verta/blobs/testdir/testfile")
        ))
        assert(checkEqualFile(
          new File("./src/test/scala/ai/verta/blobs/testdir/testsubdir/testfile2"),
          new File("./somefiles/src/test/scala/ai/verta/blobs/testdir/testsubdir/testfile2")
        ))

    } finally {
      cleanup(f)
    }
  }

  test("downloading a versioned blob should recover the original content") {
    val f = fixture

    try {
      val originalContent = generateRandomFile("somefile").get
      val pathBlob = PathBlob("somefile", true).get
      val commit = f.commit
        .update("file", pathBlob)
        .flatMap(_.save("some-msg")).get

      // create a new file with same name
      val updatedContent = generateRandomFile("somefile").get
      // check that the content is now different:
      assert(!Files.readAllBytes((new File("somefile")).toPath).sameElements(originalContent))

      // recover the old versioned file:
      val retrievedBlob: Dataset = commit.get("file").get match {
        case path: PathBlob => path
      }
      retrievedBlob.download(Some("somefile"), "somefile")
      assert(Files.readAllBytes((new File("somefile")).toPath).sameElements(originalContent))
    } finally {
      cleanup(f)
    }
  }
}
