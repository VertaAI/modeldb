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
      f.commit.update("abc/def", f.pathBlob)

      val getAttempt = f.commit.get("abc/def").get

      // check that the content of the pathblob is not corrupted:
      val pathBlob2 = getAttempt match {
        case blob: PathBlob => blob
      }
      assert(pathBlob2 equals f.pathBlob)

      val getAttempt2 = f.commit.get("xyz/tuv")
      assert(getAttempt2.isFailure)
      assert(getAttempt2 match {case Failure(e) => e.getMessage contains "No blob was stored at this path."})
    } finally {
      cleanup(f)
    }
  }

  test("Tagging unsaved commit should fail") {
    val f = fixture

    try {
      f.commit.update("abc/def", f.pathBlob)
      val tagAttempt = f.commit.tag("Some tag")
      assert(tagAttempt.isFailure)
      assert(tagAttempt match {
        case Failure(e) => e.getMessage contains "Commit must be saved before it can be tagged"
      })
    } finally {
      cleanup(f)
    }
  }

  test("newBranch unsaved commit should fail") {
    val f = fixture
    
    try {
      f.commit.update("abc/def", f.pathBlob)
      val newBranchAttempt = f.commit.newBranch("some-branch")
      assert(newBranchAttempt.isFailure)
      assert(newBranchAttempt match {
        case Failure(e) => e.getMessage contains "Commit must be saved before it can be attached to a branch"
      })
    } finally {
      cleanup(f)
    }
  }
}
