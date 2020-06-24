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
        val pathBlob = PathBlob(f"${System.getProperty("user.dir")}/src/test/scala/ai/verta/blobs/testdir").get
        val s3Blob = S3(S3Location("s3://verta-scala-test/testdir/testsubdir/testfile2").get).get
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
      f.commit.update("abc", PathBlob(f"${System.getProperty("user.dir")}/src/test/scala/ai/verta/blobs/testdir", true).get).flatMap(_.save("some-msg"))
    } finally {
      cleanup(f)
    }
  }
}
