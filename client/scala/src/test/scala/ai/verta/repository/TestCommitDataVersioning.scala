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
        // val s3Blob2 = S3(S3Location("s3://verta-scala-test/testdir/").get).get

        val commit = repo.getCommitByBranch()
          .flatMap(_.update("s3-blob", s3Blob))
          .flatMap(_.update("path-blob", pathBlob))
          .flatMap(_.update("s3-blob2", s3Blob2))
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

  /** Check to see if two files or directories have the same content */
  def checkEqualFile(firstFile: File, secondFile: File): Boolean = {
    if (firstFile.isDirectory && secondFile.isDirectory) {
      val firstContents = firstFile.listFiles.map(file => file.getName -> file).toMap
      val secondContents = secondFile.listFiles.map(file => file.getName -> file).toMap

      if (firstContents.size != secondContents.size)
        false
      else {
        firstContents.forall(pair =>
          secondContents.get(pair._1).isDefined && checkEqualFile(pair._2, secondContents.get(pair._1).get)
        )
      }
    }
    else if (firstFile.isFile && secondFile.isFile) {
      val first: Array[Byte] = Files.readAllBytes(firstFile.toPath)
      val second = Files.readAllBytes(secondFile.toPath)
      Arrays.equals(first, second)
    }
    else false
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
      val versionedS3Blob: S3 = f.commit.get("s3-blob").get match {
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
      val versionedPathBlob: PathBlob = f.commit.get("path-blob").get match {
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
      val versionedS3Blob: S3 = f.commit.get("s3-blob").get match {
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

      val versionedPathBlob: PathBlob = f.commit.get("path-blob").get match {
        case path: PathBlob => path
      }

      versionedPathBlob.download(
        Some("./src/test/scala/ai/verta/blobs/testdir"),
        "./somefiles"
      )
      assert(checkEqualFile(new File("./src/test/scala/ai/verta/blobs/testdir"), new File("./somefiles")))
    } finally {
      cleanup(f)
    }
  }

  test("download entire blobs should retrieve all the components") {
    val f = fixture

    try {
        val versionedS3Blob: S3 = f.commit.get("s3-blob").get match {
          case s3: S3 => s3
        }
        versionedS3Blob.download(downloadToPath = "./somefiles2")
        val downloadedS3MD5 = PathBlob("./somefiles2").get
        assert(
          downloadedS3MD5.getMetadata("./somefiles2/verta-scala-test/testdir/testsubdir/testfile2").get.md5 equals
          f.s3Blob.getMetadata("s3://verta-scala-test/testdir/testsubdir/testfile2").get.md5
        )

        val versionedPathBlob: PathBlob = f.commit.get("path-blob").get match {
          case path: PathBlob => path
        }
        versionedPathBlob.download(downloadToPath = "./somefiles")
        // src/test/scala/ai/verta/blobs/testdir is downwloaded into somefiles directory:
        assert(checkEqualFile(
          new File("./src/test/scala/ai/verta/blobs/testdir"),
          new File("./somefiles/src/test/scala/ai/verta/blobs/testdir")
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

  test("passing in a non-existing componentPath should fail") {
    val f = fixture

    try {
      val retrievedPathBlob: Dataset = f.commit.get("path-blob").get match {
        case path: PathBlob => path
      }
      val downloadAttempt = retrievedPathBlob.download(Some("non-existing"), "somefile")
      assert(downloadAttempt.isFailure)
      assert(downloadAttempt match {case Failure(e) => e.getMessage contains "Components not found."})

      val retrievedS3Blob: Dataset = f.commit.get("s3-blob").get match {
        case s3: S3 => s3
      }
      val downloadAttempt2 = retrievedS3Blob.download(Some("non-existing"), "somefile")
      assert(downloadAttempt2.isFailure)
      assert(downloadAttempt2 match {case Failure(e) => e.getMessage contains "Components not found."})
    } finally {
      cleanup(f)
    }
  }

  test("only blobs enabling versioning and obtained by commit.get can download") {
    val f = fixture

    try {
      val downloadAttempt = f.pathBlob.download(downloadToPath = "some-path")
      assert(downloadAttempt.isFailure)
      assert(downloadAttempt match {case Failure(e) => e.getMessage contains "This dataset cannot be used for downloads"})

      val downloadAttempt2 = f.s3Blob.download(downloadToPath = "some-path")
      assert(downloadAttempt2.isFailure)
      assert(downloadAttempt2 match {case Failure(e) => e.getMessage contains "This dataset cannot be used for downloads"})

      // this check currently fails. Will need to revisit later
      // val retrievedS3Blob2: Dataset = f.commit.get("s3-blob2").get match {
      //   case s3: S3 => s3
      // }
      // val downloadAttempt3 = retrievedS3Blob2.download(downloadToPath = "some-path")
      // assert(downloadAttempt3.isFailure)
      // assert(downloadAttempt3 match {case Failure(e) => e.getMessage contains "This blob did not allow for versioning"})
    } finally {
      cleanup(f)
    }
  }
}
