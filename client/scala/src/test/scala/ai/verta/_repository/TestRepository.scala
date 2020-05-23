package ai.verta._repository
import ai.verta.client._
import ai.verta.swagger.client.HttpException

import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestRepository extends FunSuite {
  implicit val ec = ExecutionContext.global

  test("get or create") {
    val client = new Client(ClientConnection.fromEnvironment())

    try {
      assert(client.getOrCreateRepository("New Repo").isInstanceOf[Success[Repository]])
    } finally {
      client.close()
    }
  }

  test("get repo by id (not exist)") {
    val client = new Client(ClientConnection.fromEnvironment())

    try {
      assert(client.getRepository("124112413").isInstanceOf[Failure[HttpException]])
    } finally {
      client.close()
    }
  }

  test("get repo by id") {
    val client = new Client(ClientConnection.fromEnvironment())

    try {
      assert(client.getRepository("3").isInstanceOf[Success[Repository]])
    } finally {
      client.close()
    }
  }

  test("get commit by id") {
    val client = new Client(ClientConnection.fromEnvironment())

    try {
        assert(
          client.getOrCreateRepository("New Repo")
          .flatMap(_.getCommitById("f502d423d86df839bd5d1aba2ee04dcc52d4292980e89573faef649fdd643b03"))
          .isInstanceOf[Success[Commit]]
        )
    } finally {
      client.close()
    }
  }

}
