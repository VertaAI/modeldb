package ai.verta.repository

import ai.verta.client._
import ai.verta.blobs._
import ai.verta.blobs.dataset._

import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import scala.util.{Try, Success, Failure}

import java.io.File

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
        val s3Blob = S3(S3Location("s3://verta-scala-test/testdir/testsubdir/testfile2").get, true).get
    }

  def cleanup(
    f: AnyRef{val client: Client; val repo: Repository; val commit: Commit; val pathBlob: PathBlob; val s3Blob: S3}
  ) = {
    f.client.deleteRepository(f.repo.id)
    f.client.close()
  }

  test("versioning") {
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
        "s3://verta-scala-test/testdir/testsubdir/testfile2",
        "./somefile2"
      )
      assert(downloadS3Attempt.isSuccess)

      val versionedPathBlob: PathBlob = newCommit.get("def").get match {
        case path: PathBlob => path
      }
      val downloadPathAttempt = versionedPathBlob.download(
        "./src/test/scala/ai/verta/blobs/testdir/testfile",
        "./somefile"
      )
      assert(downloadPathAttempt.isSuccess)
    } finally {
      cleanup(f)
    }
  }
}
