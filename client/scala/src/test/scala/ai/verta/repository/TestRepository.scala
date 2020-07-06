package ai.verta.repository
import ai.verta.client._
import ai.verta.swagger.client.HttpException

import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestRepository extends FunSuite {
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

  test("get/create by name should return a repository with correct name") {
    val f = fixture

    try {
      assert(f.client.getOrCreateRepository("My Repo").get.name.equals("My Repo"))
    } finally {
      cleanup(f)
    }
  }

  test("get repo by id (not exist) should fail") {
    val f = fixture

    try {
      val getRepoAttempt = f.client.getRepository(BigInt("124112413"))

      assert(getRepoAttempt.isFailure)
      // check if the correct error is returned:
      assert(getRepoAttempt match {case Failure(e) => e.getMessage contains "Couldn't find repository by id"})
    } finally {
      cleanup(f)
    }
  }

  test("get repo by id should return a repository with correct id") {
    val f = fixture

    try {
      val getRepoAttempt = f.client.getRepository(f.repo.id)

      assert(getRepoAttempt.isSuccess)
      assert(getRepoAttempt.get equals f.repo)
    } finally {
      cleanup(f)
    }
  }

  test("get commit by id should return a commit with correct id (commit sha)") {
    val f = fixture

    try {
      val originalCommit = f.repo.getCommitByBranch().get
      val getCommitAttempt = f.repo.getCommitById(originalCommit.id.get)

      assert(getCommitAttempt.isSuccess)
      assert(getCommitAttempt.get equals originalCommit)
    } finally {
      cleanup(f)
    }
  }

  test("get commit by id (not exist) should return a failure") {
    val f = fixture

    try {
      val getCommitAttempt = f.repo.getCommitById("1234123")

      assert(getCommitAttempt.isFailure)
      assert(getCommitAttempt match {
        case Failure(e) => e.getMessage contains "Commit_hash and repository_id mapping not found"
      })
    } finally {
      cleanup(f)
    }
  }

  test("get commit by branch without argument should get master branch") {
    val f = fixture

    try {
      val commitNoInput = f.repo.getCommitByBranch().get
      val commitMaster = f.repo.getCommitByBranch("master").get

      assert(commitNoInput equals commitMaster)
    } finally {
      cleanup(f)
    }
  }

  test("create new branch and get commit by that branch's name") {
    val f = fixture

    try {
      val commit = f.repo.getCommitByBranch().get
      val commitNewBranch = commit.newBranch("new-branch").get
      val commitGetByBranch = f.repo.getCommitByBranch("new-branch").get

      assert(commitGetByBranch equals commitNewBranch)
    } finally {
      cleanup(f)
    }
  }

  test("get commit by branch (not exist) should return a failure")  {
    val f = fixture

    try {
      val getCommitAttempt = f.repo.getCommitByBranch("not-exist")

      assert(getCommitAttempt.isFailure)
      assert(getCommitAttempt match {
        case Failure(e) => e.getMessage contains "Branch not found"
      })
    } finally {
      cleanup(f)
    }
  }

  test("add/remove tag and get commit by tag") {
    val f = fixture

    try {
      val commit = f.repo.getCommitByBranch().get
      commit.tag("Some tag")

      val getCommitAttempt = f.repo.getCommitByTag("Some tag")
      assert(getCommitAttempt.isSuccess)
      assert(getCommitAttempt.get equals commit)

      f.repo.deleteTag("Some tag")

      val getCommitAttemptAfterDel = f.repo.getCommitByTag("Some tag")
      assert(getCommitAttemptAfterDel.isFailure)
      assert(getCommitAttemptAfterDel match {case Failure(e) => e.getMessage contains "Tag not found"})
    } finally {
      cleanup(f)
    }
  }
}
