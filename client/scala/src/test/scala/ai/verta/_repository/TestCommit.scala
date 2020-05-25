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

class TestCommit extends FunSuite {
  implicit val ec = ExecutionContext.global

  def fixture =
    new {
        val client = new Client(ClientConnection.fromEnvironment())
        client.getOrCreateRepository("New Repo")
        val commit = client.getOrCreateRepository("New Repo")
        .flatMap(_.getCommitByBranch()).get
    }

  def cleanup(f: AnyRef{val client: Client; val commit: Commit}) = {
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


  test("update commit") {
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

}
