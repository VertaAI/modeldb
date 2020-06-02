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
    f.client.deleteRepository(f.repo.repo.id.get.toString)
    f.client.close()
  }

  test("get/create by name should return a repository with correct name") {
    val f = fixture

    try {
      assert(f.client.getOrCreateRepository("My Repo").get.repo.name.get.equals("My Repo"))
    } finally {
      cleanup(f)
    }
  }

  test("get repo by id (not exist) should fail") {
    val f = fixture

    try {
      val getRepoAttempt = f.client.getRepository("124112413")
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
      val getRepoAttempt = f.client.getRepository(f.repo.repo.id.get.toString)
      assert(getRepoAttempt.isSuccess)
      assert(getRepoAttempt.get.repo.id.get.equals(f.repo.repo.id.get))
    } finally {
      cleanup(f)
    }
  }

  test("get commit by id should return a commit with correct id (commit sha))") {
    val f = fixture

    try {
      val id = f.repo.getCommitByBranch().map(_.commit).get.commit_sha.get

      val getCommitAttempt = f.repo.getCommitById(id)
      assert(getCommitAttempt.isSuccess)
      assert(getCommitAttempt.get.commit.commit_sha.get.equals(id))
    } finally {
      cleanup(f)
    }
  }

  test("get commit by branch without argument should get master branch") {
    val f = fixture

    try {
      val commitNoInput = f.repo.getCommitByBranch().get
      val commitMaster = f.repo.getCommitByBranch("master").get

      val commitNoInputSHA = commitNoInput.commit.commit_sha.get
      val commitMasterSHA = commitMaster.commit.commit_sha.get

      assert(commitNoInputSHA.equals(commitMasterSHA))
    } finally {
      cleanup(f)
    }
  }
}
