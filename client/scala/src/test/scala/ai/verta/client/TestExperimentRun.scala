package ai.verta.client

import ai.verta.repository._
import ai.verta.blobs._
import ai.verta.blobs.dataset._

import scala.language.reflectiveCalls
import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestExperimentRun extends FunSuite {
  implicit val ec = ExecutionContext.global

  def fixture =
    new {
      val client = new Client(ClientConnection.fromEnvironment())
      val repo = client.getOrCreateRepository("My Repo").get
    }

  def cleanup(f: AnyRef{val client: Client; val repo: Repository}) = {
    f.client.deleteRepository(f.repo.id)
    f.client.close()
  }

  test("get commit should retrieve the right commit that was logged") {
    val f = fixture

    try {
      val commit = f.repo.getCommitByBranch().get
      val pathBlob = PathBlob(f"${System.getProperty("user.dir")}/src/test/scala/ai/verta/blobs/testdir").get

      val expRun = f.client.getOrCreateProject("scala test")
        .flatMap(_.getOrCreateExperiment("experiment"))
        .flatMap(_.getOrCreateExperimentRun()).get

      val logAttempt = expRun.logCommit(commit)
      assert(logAttempt.isSuccess)

      val expRunCommit = expRun.getCommit().get
      val retrievedCommit = f.client.getRepository(expRunCommit.repoId)
                             .flatMap(_.getCommitById(expRunCommit.commitSHA)).get

      assert(retrievedCommit equals commit)

      val newCommit = commit.update("abc/def", pathBlob)
                            .flatMap(_.save("Add a blob")).get
      expRun.logCommit(newCommit, Some(Map[String, String]("mnp/qrs" -> "abc/def")))
      val newExpRunCommit = expRun.getCommit().get
      val newRetrievedCommit =  f.client.getRepository(newExpRunCommit.repoId)
                                 .flatMap(_.getCommitById(newExpRunCommit.commitSHA)).get
      assert(newCommit equals newRetrievedCommit)
      assert(!newRetrievedCommit.equals(commit))
      assert(newExpRunCommit.keyPaths.get equals Map[String, String]("mnp/qrs" -> "abc/def"))
    } finally {
      cleanup(f)
    }
  }

  test("get commit should fail if there isn't one assigned to the run") {
    val f = fixture

    try {
      val expRun = f.client.getOrCreateProject("scala test")
        .flatMap(_.getOrCreateExperiment("some-experiment"))
        .flatMap(_.getOrCreateExperimentRun()).get

      val getAttempt = expRun.getCommit()
      assert(getAttempt.isFailure)
      assert(getAttempt match {case Failure(e) => e.getMessage contains "No commit is associated with this experiment run"})
    } finally {
      cleanup(f)
    }
  }
}
