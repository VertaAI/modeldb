package ai.verta.repository

import ai.verta.client._
import ai.verta.blobs._
import ai.verta.blobs.dataset._

import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import scala.util.{Try, Success, Failure}

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Arrays

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestCommitDataVersioning extends FunSuite {
  implicit val ec = ExecutionContext.global

  def fixture =
    new {
        val client = new Client(ClientConnection.fromEnvironment())
        val repo = client.getOrCreateRepository("My Repo").get
        val commit = repo.getCommitByBranch().get

        val pathBlob = PathBlob("./src/test/scala/ai/verta/blobs/testdir", true).get
        val s3Blob = S3(S3Location("s3://verta-scala-test/testdir/").get, true).get
    }

  def cleanup(
    f: AnyRef{val client: Client; val repo: Repository; val commit: Commit; val pathBlob: PathBlob; val s3Blob: S3}
  ) = {
    f.client.deleteRepository(f.repo.id)
    f.client.close()
  }

  def deleteDirectory(dir: File): Unit = {
    Option(dir.listFiles()).map(_.foreach(deleteDirectory))
    dir.delete()
  }

  def checkEqualFile(firstFile: File, secondFile: File) = {
    val first: Array[Byte] = Files.readAllBytes(firstFile.toPath)
    val second = Files.readAllBytes(secondFile.toPath)
    Arrays.equals(first, second)
  }

  test("downloading an uploaded file should not corrupt it") {
    val f = fixture

    try {
      val newCommit = f.commit
        .update("abc", f.s3Blob)
        .flatMap(_.update("def", f.pathBlob))
        .flatMap(_.save("some-msg")).get

      // check for s3:
      val versionedS3Blob: S3 = newCommit.get("abc").get match {
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
      val versionedPathBlob: PathBlob = newCommit.get("def").get match {
        case path: PathBlob => path
      }
      val downloadPathAttempt = versionedPathBlob.download(
        Some("./src/test/scala/ai/verta/blobs/testdir/testfile"),
        "./somefile"
      )
      assert(downloadPathAttempt.isSuccess)
      checkEqualFile(
        new File("./src/test/scala/ai/verta/blobs/testdir/testfile"),
        new File("./somefile")
      )
    } finally {
      (new File("./somefile")).delete()
      (new File("./somefile2")).delete()
      cleanup(f)
    }
  }

  test("downloading an entire folder should retrieve all the files in the folder") {
    val f = fixture

    try {
      val newCommit = f.commit
        .update("abc", f.s3Blob)
        .flatMap(_.update("def", f.pathBlob))
        .flatMap(_.save("some-msg")).get

      val versionedS3Blob: S3 = newCommit.get("abc").get match {
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


      val versionedPathBlob: PathBlob = newCommit.get("def").get match {
        case path: PathBlob => path
      }

      versionedPathBlob.download(
        Some("./src/test/scala/ai/verta/blobs/testdir"),
        "./somefiles"
      )
      checkEqualFile(
        new File("./src/test/scala/ai/verta/blobs/testdir/testfile"),
        new File("./somefiles/testfile")
      )

      checkEqualFile(
        new File("./src/test/scala/ai/verta/blobs/testdir/testsubdir/testfile2"),
        new File("./somefiles/testsubdir/testfile2")
      )
    } finally {
      deleteDirectory(new File("./somefiles"))
      deleteDirectory(new File("./somefiles2"))
      cleanup(f)
    }
  }

  test("download entire blobs should retrieve all the components") {
    val f = fixture

    try {
      val newCommit = f.commit
        .update("abc", f.s3Blob)
        .flatMap(_.update("def", f.pathBlob))
        .flatMap(_.save("some-msg")).get

        val versionedS3Blob: S3 = newCommit.get("abc").get match {
          case s3: S3 => s3
        }
        versionedS3Blob.download(downloadToPath = "./somefiles2")
        val downloadedS3MD5 = PathBlob("./somefiles2").get
        assert(
          downloadedS3MD5.getMetadata("./somefiles2/verta-scala-test/testdir/testsubdir/testfile2").get.md5 equals
          f.s3Blob.getMetadata("s3://verta-scala-test/testdir/testsubdir/testfile2").get.md5
        )

        val versionedPathBlob: PathBlob = newCommit.get("def").get match {
          case path: PathBlob => path
        }
        versionedPathBlob.download(downloadToPath = "./somefiles")
        checkEqualFile(
          new File("./src/test/scala/ai/verta/blobs/testdir/testfile"),
          new File("./somefiles/src/test/scala/ai/verta/blobs/testdir/testfile")
        )
        checkEqualFile(
          new File("./src/test/scala/ai/verta/blobs/testdir/testsubdir/testfile2"),
          new File("./somefiles/src/test/scala/ai/verta/blobs/testdir/testsubdir/testfile2")
        )

    } finally {
      deleteDirectory(new File("./somefiles"))
      deleteDirectory(new File("./somefiles2"))
      cleanup(f)
    }
  }
}
