package ai.verta._repository
import ai.verta.client._
import ai.verta.swagger.client.HttpException
import ai.verta.swagger._public.modeldb.versioning.model.VersioningSetTagRequestResponse

import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestCommit extends FunSuite {
  implicit val ec = ExecutionContext.global

  // test ("add a tag to commit") {
  //   val client = new Client(ClientConnection.fromEnvironment())
  //
  //   try {
  //         assert(client.getOrCreateRepository("New Repo")
  //           .flatMap(_.getCommitById("f502d423d86df839bd5d1aba2ee04dcc52d4292980e89573faef649fdd643b03"))
  //           .flatMap(_.tag("Some tag"))
  //           .isInstanceOf[Success[VersioningSetTagRequestResponse]]
  //         )
  //   } finally {
  //     client.close()
  //   }
  //
  // }

  test("get commit's log") {
    val client = new Client(ClientConnection.fromEnvironment())

    try {
        assert(
          client.getOrCreateRepository("New Repo")
          .flatMap(_.getCommitById("f502d423d86df839bd5d1aba2ee04dcc52d4292980e89573faef649fdd643b03"))
          .flatMap(_.log())
          .isInstanceOf[Success[List[Commit]]]
        )
    } finally {
      client.close()
    }
  }

}
