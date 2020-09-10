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

  test("add and retrieve version's attributes") {
    val f = fixture

    try {
      val workingDir = System.getProperty("user.dir")
      val testDir = workingDir + "/src/test/scala/ai/verta/blobs/testdir"
      val version = f.dataset.createPathVersion(List(testDir)).get

      version.addAttribute("some", 0.5)
      version.addAttribute("int", 4)
      version.addAttributes(Map("other" -> 0.3, "string" -> "desc"))

      assert(version.getAttribute("some").get.get.asDouble.get equals 0.5)
      assert(version.getAttribute("other").get.get.asDouble.get equals 0.3)
      assert(version.getAttribute("int").get.get.asBigInt.get equals 4)
      assert(version.getAttribute("string").get.get.asString.get equals "desc")

      assert(version.getAttributes().get.equals(
        Map[String, ValueType]("some" -> 0.5, "int" -> 4, "other" -> 0.3, "string" -> "desc")
      ))
    } finally {
      cleanup(f)
    }
  }
}
