package ai.verta.dataset_versioning

import ai.verta.client._
import ai.verta.dataset_versioning._

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

  test("retrieve dataset by id") {
    val f = fixture

    try {
      assert(f.client.getDatasetById(f.dataset.id).get.id == f.dataset.id)
    } finally {
      cleanup(f)
    }
  }

  test("retrieve dataset by name") {
    val f = fixture

    try {
      assert(f.client.getDatasetByName(f.dataset.name).get.id == f.dataset.id)
    } finally {
      cleanup(f)
    }
  }

  test("retrieve dataset with wrong name or id should fail") {
    val f = fixture

    try {
      val getByNameAttempt = f.client.getDatasetByName("wrong-name")
      assert(getByNameAttempt match {
        case Failure(e) => e.getMessage contains "not found"
      })

      val getByIdAttempt = f.client.getDatasetById("wrong-id")
      assert(getByIdAttempt match {
        case Failure(e) => e.getMessage contains "not found"
      })
    } finally {
      cleanup(f)
    }
  }
}
