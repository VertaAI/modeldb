package ai.verta._repository
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
        val repo = client.getOrCreateRepository("New Repo").get
    }

  def cleanup(f: AnyRef{val client: Client; val repo: Repository}) = {
    f.client.close()
  }

  test("get/create by name") {
    val f = fixture

    try {
      assert(f.client.getOrCreateRepository("New Repo").isInstanceOf[Success[Repository]])
    } finally {
      cleanup(f)
    }
  }

  test("get repo by id (not exist)") {
    val f = fixture

    try {
      assert(f.client.getRepository("124112413").isInstanceOf[Failure[HttpException]])
    } finally {
      cleanup(f)
    }
  }

  test("get repo by id") {
    val f = fixture

    try {
      assert(f.client.getRepository(f.repo.repo.id.get.toString)
      .isInstanceOf[Success[Repository]])
    } finally {
      cleanup(f)
    }
  }

  test("get commit by id") {
    val f = fixture

    try {
      val id = f.repo
      .getCommitByBranch()
      .map(_.commit).get.commit_sha.get

      assert(
        f.client.getOrCreateRepository("New Repo")
        .flatMap(_.getCommitById(id))
        .isInstanceOf[Success[Commit]]
      )
    } finally {
      cleanup(f)
    }
  }

  test("get commit on master branch") {
    val f = fixture

    try {
      assert(
        f.repo.getCommitByBranch()
        .isInstanceOf[Success[Commit]]
      )
    } finally {
      cleanup(f)
    }
  }


  // test("get commit on a tag branch") {
  //   val f = fixture
  //
  //   try {
  //     assert(
  //       f.repo
  //       .flatMap(_.getCommitByTag("Some tag"))
  //       .isInstanceOf[Success[Commit]]
  //     )
  //   } finally {
  //     cleanup(f)
  //   }
  // }
}
