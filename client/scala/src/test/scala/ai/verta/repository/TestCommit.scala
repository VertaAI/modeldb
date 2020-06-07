package ai.verta.repository

import ai.verta.client._
import ai.verta.blobs._
import ai.verta.blobs.dataset._
import ai.verta.swagger.client.HttpException
import ai.verta.swagger._public.modeldb.versioning.model.VersioningSetTagRequestResponse

import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

import scala.collection.mutable.HashSet

/**
 */
class TestCommit extends FunSuite {
  implicit val ec = ExecutionContext.global

  def fixture =
    new {
        val client = new Client(ClientConnection.fromEnvironment())
        val repo = client.getOrCreateRepository("My Repo").get
        val commit = repo.getCommitByBranch().get
    }

  def cleanup(f: AnyRef{val client: Client; val repo: Repository; val commit: Commit}) = {
    f.client.deleteRepository(f.repo.id)
    f.client.close()
  }

  test("Get should retrieve only blobs that were updated") {
    val f = fixture

    try {
      val workingDir = System.getProperty("user.dir")
      val testDir = workingDir + "/src/test/scala/ai/verta/blobs/testdir"
      val pathBlob = PathBlob(List(testDir)).get
      f.commit.update("abc/def", pathBlob)

      val getAttempt = f.commit.get("abc/def")
      assert(getAttempt.isDefined)

      // check that the content of the pathblob is not corrupted:
      val pathBlob2 = getAttempt.get match {
        case blob: PathBlob => blob
      }
      assert(pathBlob2 equals pathBlob)

      assert(f.commit.get("tuv/wxy").isEmpty)
    } finally {
      cleanup(f)
    }
  }
}
