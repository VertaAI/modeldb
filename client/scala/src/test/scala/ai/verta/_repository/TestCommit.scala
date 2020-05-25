package ai.verta._repository

import ai.verta.client._
import ai.verta.blobs._
import ai.verta.swagger.client.HttpException
import ai.verta.swagger._public.modeldb.versioning.model.VersioningSetTagRequestResponse

import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestCommit extends FunSuite {
  implicit val ec = ExecutionContext.global

  test("get commit's log") {
    val client = new Client(ClientConnection.fromEnvironment())

    try {
        assert(
          client.getOrCreateRepository("New Repo")
          .flatMap(_.getCommitByBranch())
          .flatMap(_.log())
          .isInstanceOf[Success[List[Commit]]]
        )
    } finally {
      client.close()
    }
  }


  test("update commit") {
    val client = new Client(ClientConnection.fromEnvironment())

    try {
        var commit = client.getOrCreateRepository("New Repo")
        .flatMap(_.getCommitByBranch("master"))
        .get

        commit.update("abc/cde", Git(hash = Some("abc"), repo = Some("abc")))
        assert(commit.save("Some message").isInstanceOf[Success[_]])
    } finally {
      client.close()
    }
  }

}
