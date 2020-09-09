package ai.verta.dataset_versioning

import ai.verta.client._
import ai.verta.dataset_versioning._
import ai.verta.blobs.dataset.S3Location

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

  test("retrieve dataset version") {
    val f = fixture

    try {
      val workingDir = System.getProperty("user.dir")
      val testDir = workingDir + "/src/test/scala/ai/verta/blobs/testdir"
      val version = f.dataset.createPathVersion(List(testDir)).get

      assert(f.dataset.getVersion(version.id).get.id == version.id)
      assert(f.dataset.getLatestVersion().get.id == version.id)

      val testfilePath = "s3://verta-scala-test/testdir/testfile"
      val testfileLoc = S3Location(testfilePath).get
      val newVersion = f.dataset.createS3Version(List(testfileLoc)).get
      val latestVersion = f.dataset.getLatestVersion().get

      assert(latestVersion.id == newVersion.id)
      assert(latestVersion.id != version.id)
    } finally {
      cleanup(f)
    }
  }
}
