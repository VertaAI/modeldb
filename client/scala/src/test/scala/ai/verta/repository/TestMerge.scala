package ai.verta.repository

import ai.verta.client._
import ai.verta.blobs._
import ai.verta.blobs.dataset._

import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestMerge extends FunSuite {
  implicit val ec = ExecutionContext.global

  def fixture =
    new {
        val client = new Client(ClientConnection.fromEnvironment())
        val repo = client.getOrCreateRepository("My Repo").get
        val commit = repo.getCommitByBranch().get
        val pathBlob = PathBlob(List(
          f"${System.getProperty("user.dir")}/src/test/scala/ai/verta/blobs/testdir/testfile"
        )).get

        val pathBlob2 = PathBlob(List(
          f"${System.getProperty("user.dir")}/src/test/scala/ai/verta/blobs/testdir/testsubdir/testfile2"
        )).get
    }

  def cleanup(
    f: AnyRef{
      val client: Client; val repo: Repository; val commit: Commit;
      val pathBlob: PathBlob; val pathBlob2: PathBlob
    }
  ) = {
    f.client.deleteRepository(f.repo.id)
    f.client.close()
  }

  test("merge two commits with no conflicts") {
    val f = fixture

    try {
        val branch1 = f.repo.getCommitByBranch()
                       .flatMap(_.newBranch("a"))
                       .flatMap(_.update("abc/cde", f.pathBlob))
                       .flatMap(_.save("Some message 1")).get

        val branch2 = f.repo.getCommitByBranch()
                       .flatMap(_.newBranch("b"))
                       .flatMap(_.update("def/ghi", f.pathBlob2))
                       .flatMap(_.save("Some message 2")).get

        val mergeAttempt = branch1.merge(branch2, message = Some("Merge test"))
        assert(mergeAttempt.isSuccess)

        val mergedCommit = mergeAttempt.get

    } finally {
      cleanup(f)
    }
  }

  test("merge two commits with conflicts") {
    val f = fixture

    try {
      val branch1 = f.repo.getCommitByBranch()
                     .flatMap(_.newBranch("a"))
                     .flatMap(_.update("abc/cde", f.pathBlob))
                     .flatMap(_.save("Some message 1")).get

      val branch2 = f.repo.getCommitByBranch()
                     .flatMap(_.newBranch("b"))
                     .flatMap(_.update("abc/cde", f.pathBlob2))
                     .flatMap(_.save("Some message 2")).get

        val mergeAttempt = branch1.merge(branch2, message = Some("Merge test"))
    } finally {
      cleanup(f)
    }
  }

  test("merge unsaved commits should return exception") {
    val f = fixture

    try {
      val branch1 = f.repo.getCommitByBranch()
                     .flatMap(_.newBranch("a"))
                     .flatMap(_.update("abc/cde", f.pathBlob))
                     .flatMap(_.save("Some message 1")).get

      val branch2 = f.repo.getCommitByBranch()
                     .flatMap(_.newBranch("b"))
                     .flatMap(_.update("def/ghi", f.pathBlob2)).get

        val mergeAttempt = branch1.merge(branch2, message = Some("Merge test"))
        assert(mergeAttempt.isFailure)

        val mergeAttempt2 = branch2.merge(branch1, message = Some("Merge test"))
        assert(mergeAttempt2.isFailure)
    } finally {
      cleanup(f)
    }
  }
}
