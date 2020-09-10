package ai.verta.dataset_versioning

import ai.verta.client._
import ai.verta.dataset_versioning._
import ai.verta.blobs.dataset.S3Location
import ai.verta.client.entities.utils.ValueType

import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestDataset extends FunSuite {
  implicit val ec = ExecutionContext.global

  def fixture =
    new {
        val client = new Client(ClientConnection.fromEnvironment())
        val dataset = client.getOrCreateDataset("my dataset").get
    }

  def cleanup(
    f: AnyRef{val client: Client; val dataset: Dataset}
  ) = {
    f.client.deleteDataset(f.dataset.id)
    f.client.close()
  }

  test("Dataset tags CRUD") {
    val f = fixture

    try {
      f.dataset.addTag("tag-1")
      f.dataset.addTags(List("tag-2", "tag-3"))

      assert(f.dataset.getTags().get == List("tag-1", "tag-2", "tag-3"))

      f.dataset.delTags(List("tag-2", "tag-4"))

      assert(f.dataset.getTags().get == List("tag-1", "tag-3"))

    } finally {
      cleanup(f)
    }
  }
}
