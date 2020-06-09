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
        val pathBlob = PathBlob(f"${System.getProperty("user.dir")}/src/test/scala/ai/verta/blobs/testdir").get
        val s3Blob = S3(S3Location("s3://verta-scala-test/testdir/testsubdir/testfile2").get).get
    }

  def cleanup(
    f: AnyRef{val client: Client; val repo: Repository; val commit: Commit; val pathBlob: PathBlob; val s3Blob: S3}
  ) = {
    f.client.deleteRepository(f.repo.id)
    f.client.close()
  }

  test("Get should retrieve blobs that were updated") {
    val f = fixture

    try {
      val newCommit = f.commit.update("abc/def", f.pathBlob)
                              .flatMap(_.update("mnp/qrs", f.s3Blob)).get


      // check that the contents of the blobs are not corrupted:
      val pathBlob2 = newCommit.get("abc/def").get match {
        case blob: PathBlob => blob
        case blob: S3 => blob
      }
      assert(pathBlob2 equals f.pathBlob)

      val s3Blob2 = newCommit.get("mnp/qrs").get match {
        case blob: PathBlob => blob
        case blob: S3 => blob
      }
      assert(s3Blob2 equals f.s3Blob)
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

  test("Tagging unsaved commit should fail") {
    val f = fixture

    try {
      val newCommit = f.commit.update("abc/def", f.pathBlob).get
      val tagAttempt = newCommit.tag("Some tag")
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
      val newCommit = f.commit.update("abc/def", f.pathBlob).get
      val newBranchAttempt = newCommit.newBranch("some-branch")
      assert(newBranchAttempt.isFailure)
      assert(newBranchAttempt match {
        case Failure(e) => e.getMessage contains "Commit must be saved before it can be attached to a branch"
      })
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

  test("Saving a branched commit should update the branch's head") {
    val f = fixture

    try {
      val branchedCommit = f.commit.newBranch("some-branch").get
      val newCommit = branchedCommit.update("abc/def", f.pathBlob)
                                    .flatMap(_.save("Some msg"))
                                    .get

      // head of some-branch should be the new commit, not the old one
      assert(newCommit.equals(f.repo.getCommitByBranch("some-branch").get))
      assert(!branchedCommit.equals(f.repo.getCommitByBranch("some-branch")))
    } finally {
      cleanup(f)
    }
  }

  test("Saving should not corrupt the stored blobs") {
    val f = fixture

    try {
      val newId = f.commit.update("abc/def", f.pathBlob)
                          .flatMap(_.save("Some msg"))
                          .get.id.get

      val newCommit = f.repo.getCommitById(newId).get
      val getAttempt = newCommit.get("abc/def").get
      val pathBlob2 = getAttempt match {
        case blob: PathBlob => blob
      }
      assert(pathBlob2 equals f.pathBlob)
    } finally {
      cleanup(f)
    }
  }
}
