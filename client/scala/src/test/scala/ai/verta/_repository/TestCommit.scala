package ai.verta._repository

import ai.verta.client._
import ai.verta.blobs._
import ai.verta.swagger.client.HttpException
import ai.verta.swagger._public.modeldb.versioning.model.VersioningSetTagRequestResponse

import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

import scala.collection.mutable.HashSet

/**
 * TODO: write test for diffs, save
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
    f.client.deleteRepository(f.repo.repo.id.get.toString)
    f.client.close()
  }

  test("get commit's log") {
    val f = fixture

    try {
        assert(
          f.commit.log()
          .isInstanceOf[Success[List[Commit]]]
        )
    } finally {
      cleanup(f)
    }
  }


  test("update and remove should properly maintain blobs") {
    val f = fixture

    try {
        f.commit.update("abc/cde", Git(hash = Some("abc"), repo = Some("abc")))
        assert(!f.commit.saved)
        assert(f.commit.get("abc/cde").isDefined)
        assert(f.commit.save("Some message 1").isInstanceOf[Success[_]])
        assert(f.commit.saved)


        f.commit.update("def/ghi", Git(hash = Some("abc"), repo = Some("abc")))
        assert(!f.commit.saved)
        assert(f.commit.get("def/ghi").isDefined)
        assert(f.commit.save("Some message 2").isInstanceOf[Success[_]])

        f.commit.remove("abc/cde")
        assert(!f.commit.saved)
        assert(f.commit.save("Some message 3").isInstanceOf[Success[_]])
        assert(f.commit.saved)
        assert(f.commit.get("abc/cde").isEmpty)

    } finally {
      cleanup(f)
    }
  }

  test("newBranch should successfully create a new branch") {
    val f = fixture

    try {
        val trySetBranch = f.commit.newBranch("new-branch")
        assert(trySetBranch.isSuccess)
        assert(trySetBranch.get.commit_branch.get.equals("new-branch"))
    } finally {
      cleanup(f)
    }
  }

  test("revert with no commit passed should successfully the latest commit") {
    val f = fixture

    try {
        f.commit.update("abc/cde", Git(hash = Some("abc"), repo = Some("abc")))
        f.commit.save("Some message 1")

        f.commit.update("def/ghi", Git(hash = Some("abc"), repo = Some("abc")))
        f.commit.save("Some message 2")
        val prevHead = f.commit.commit.commit_sha.get

        val revAttempt = f.commit.revert(message = Some("Revert test"))
        assert(revAttempt.isSuccess)
        assert(revAttempt.get.commit.message.get.equals("Revert test"))
        // previous sha should be the parent sha:
        assert(revAttempt.get.commit.parent_shas.get.head.equals(prevHead))

        assert(revAttempt.get.get("abc/cde").isDefined)
        assert(revAttempt.get.get("def/ghi").isEmpty)
    } finally {
      cleanup(f)
    }
  }

  test("merge two commits with no conflicts") {
    val f = fixture

    try {
        val branch1 = f.repo.getCommitByBranch().flatMap(_.newBranch("a")).get
        branch1.update("abc/cde", Git(hash = Some("abc"), repo = Some("abc")))
        branch1.save("Some message 1")
        val parent1 = branch1.commit.commit_sha.get

        val branch2 = f.repo.getCommitByBranch().flatMap(_.newBranch("b")).get
        branch2.update("def/ghi", Git(hash = Some("abc"), repo = Some("abc")))
        branch2.save("Some message 2")
        val parent2 = branch2.commit.commit_sha.get

        val mergeAttempt = branch1.merge(branch2, message = Some("Merge test"))
        assert(mergeAttempt.isSuccess)

        val merged = mergeAttempt.get
        assert(merged.commit.message.get.equals("Merge test"))
        // previous sha should be the parent sha:

        assert(merged.get("abc/cde").isDefined)
        assert(merged.get("def/ghi").isDefined)

        val parents = HashSet[String](parent1, parent2)
        val merged_parents = merged.commit.parent_shas.get

        assert(merged_parents.length == 2)
        assert(parents contains merged_parents.head)
        assert(parents contains merged_parents.tail.head)

    } finally {
      cleanup(f)
    }
  }

  test("merge two commits with conflicts") {
    val f = fixture

    try {
        val branch1 = f.repo.getCommitByBranch().flatMap(_.newBranch("a")).get
        branch1.update("abc/cde", Git(hash = Some("abc"), repo = Some("abc")))
        branch1.save("Some message 1")

        val branch2 = f.repo.getCommitByBranch().flatMap(_.newBranch("b")).get
        branch2.update("abc/cde", Git(hash = Some("few"), repo = Some("fwe")))
        branch2.save("Some message 2")

        val mergeAttempt = branch1.merge(branch2, message = Some("Merge test"))
        assert(mergeAttempt.isFailure)
    } finally {
      cleanup(f)
    }
  }

  test("merge unsaved commits should return exception") {
    val f = fixture

    try {
        val branch1 = f.repo.getCommitByBranch().flatMap(_.newBranch("a")).get
        branch1.update("abc/cde", Git(hash = Some("abc"), repo = Some("abc")))

        val branch2 = f.repo.getCommitByBranch().flatMap(_.newBranch("b")).get
        branch2.update("abc/cde", Git(hash = Some("few"), repo = Some("fwe")))
        branch2.save("Some message 2")

        val mergeAttempt = branch1.merge(branch2, message = Some("Merge test"))
        assert(mergeAttempt.isFailure)

        val mergeAttempt2 = branch2.merge(branch1, message = Some("Merge test"))
        assert(mergeAttempt2.isFailure)
    } finally {
      cleanup(f)
    }
  }

  test ("diffFrom and applyDiff of 2 different branches") {
    val f = fixture

    try {
        val branch1 = f.repo.getCommitByBranch().flatMap(_.newBranch("a")).get
        branch1.update("abc/cde", Git(hash = Some("abc"), repo = Some("abc")))
        branch1.save("Some message 11")
        branch1.update("wuv/ajf", Git(hash = Some("abc"), repo = Some("abc")))
        branch1.save("Some message 12")
        val parent1 = branch1.commit.commit_sha.get

        val branch2 = f.repo.getCommitByBranch().flatMap(_.newBranch("b")).get
        branch2.update("abc/cde", S3(List(new S3Location("s3://verta-scala-test"))))
        branch2.save("Some message 21")
        branch2.update("def/ghi", Git(hash = Some("abc"), repo = Some("abc")))
        branch2.save("Some message 22")
        val parent2 = branch2.commit.commit_sha.get

        val diff = branch2.diffFrom(Some(branch1)).get

        branch1.applyDiff(diff, "apply diff")
        assert(branch1.get("wuv/ajf").isEmpty)
        assert(branch1.get("def/ghi").isDefined)
        assert(branch1.get("abc/cde").get.isInstanceOf[S3])
        assert(branch1.commit.message.get.equals("apply diff"))

    } finally {
      cleanup(f)
    }
  }
}
