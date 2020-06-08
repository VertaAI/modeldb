package ai.verta.repository

import ai.verta.client._
import ai.verta.blobs._
import ai.verta.blobs.dataset._

import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestCommit extends FunSuite {
  implicit val ec = ExecutionContext.global

  def fixture =
    new {
        val client = new Client(ClientConnection.fromEnvironment())
        val repo = client.getOrCreateRepository("My Repo").get
        val commit = repo.getCommitByBranch().get
        val pathBlob = PathBlob(List(
          f"${System.getProperty("user.dir")}/src/test/scala/ai/verta/blobs/testdir"
        )).get
    }

  def cleanup(
    f: AnyRef{val client: Client; val repo: Repository; val commit: Commit; val pathBlob: PathBlob}
  ) = {
    f.client.deleteRepository(f.repo.id)
    f.client.close()
  }

  test("Get should retrieve blobs that were updated") {
    val f = fixture

    try {
      val originalId = f.commit.id
      f.commit.update("abc/def", f.pathBlob)
      assert(f.commit.save("Some message").isSuccess)

      // get the commit that was previously saved:
      val newCommit = f.repo.getCommitById(f.commit.id).get
      val originalCommit = f.repo.getCommitById(originalId).get
      assert(newCommit equals f.commit)
      assert(!newCommit.equals(originalCommit))

      // check that the content of the pathblob is not corrupted:
      val getAttempt = newCommit.get("abc/def").get
      val pathBlob2 = getAttempt match {
        case blob: PathBlob => blob
      }
      assert(pathBlob2 equals f.pathBlob)
    } finally {
      cleanup(f)
    }
  }

  test("Saving unmodified commit should fail") {
    val f = fixture

    try {
      val saveAttempt = f.commit.save("Some message")
      assert(saveAttempt.isFailure)
      assert(saveAttempt match {
        case Failure(e) => e.getMessage contains "Commit is already saved"
      })
    } finally {
      cleanup(f)
    }
  }

  test("Get with invalid paths should fail") {
    val f = fixture

    try {
      val getAttempt = f.commit.get("xyz/tuv")
      assert(getAttempt.isFailure)
      assert(getAttempt match {case Failure(e) => e.getMessage contains "No blob was stored at this path."})
    } finally {
      cleanup(f)
    }
  }
}
