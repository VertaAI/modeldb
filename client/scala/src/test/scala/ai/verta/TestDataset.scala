package ai.verta.dataset_versioning

import ai.verta.client._
import ai.verta.dataset_versioning._
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

  test("add and retrieve attributes") {
    val f = fixture

    try {
      f.dataset.addAttribute("some", 0.5)
      f.dataset.addAttribute("int", 4)
      f.dataset.addAttributes(Map("other" -> 0.3, "string" -> "desc"))

      assert(f.dataset.getAttribute("some").get.get.asDouble.get equals 0.5)
      assert(f.dataset.getAttribute("other").get.get.asDouble.get equals 0.3)
      assert(f.dataset.getAttribute("int").get.get.asBigInt.get equals 4)
      assert(f.dataset.getAttribute("string").get.get.asString.get equals "desc")

      assert(f.dataset.getAttributes().get.equals(
        Map[String, ValueType]("some" -> 0.5, "int" -> 4, "other" -> 0.3, "string" -> "desc")
      ))
    } finally {
      cleanup(f)
    }
  }
}
