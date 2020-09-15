package ai.verta.dataset_versioning

import ai.verta.client._
import ai.verta.dataset_versioning._
import ai.verta.blobs.dataset.{S3Location, AtlasHiveDatasetBlob}
import ai.verta.client.entities.utils.ValueType
import ai.verta.client.entities.utils.ValueType
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

  test("Dataset version tags CRUD") {
    val f = fixture

    try {
      val workingDir = System.getProperty("user.dir")
      val testDir = workingDir + "/src/test/scala/ai/verta/blobs/testdir"
      val version = f.dataset.createPathVersion(List(testDir)).get

      version.addTag("tag-1")
      version.addTags(List("tag-2", "tag-3"))

      assert(version.getTags().get == List("tag-1", "tag-2", "tag-3"))

      version.delTags(List("tag-2", "tag-4"))

      assert(version.getTags().get == List("tag-1", "tag-3"))
      } finally {
      cleanup(f)
    }
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
      assert(getByIdAttempt.isFailure) // message differs in OSS and dev setup.
    } finally {
      cleanup(f)
    }
  }

  test("create version from a query") {
    val f = fixture

    try {
      val query = "SELECT * FROM ner-table"
      val dbConnectionStr = "localhost:6543"
      val numRecords = 100

      val version = f.dataset.createDBVersion(query, dbConnectionStr, Some(numRecords)).get
      assert(version.id == f.dataset.getLatestVersion().get.id)
    } finally {
      cleanup(f)
    }
  }

  test("create version from an atlas hive query") {
    val f = fixture

    try {
      val guid: String = sys.env.get("GUID").get
      val version = f.dataset.createAtlasHiveVersion(guid).get

      assert(version.id == f.dataset.getLatestVersion().get.id)

      val blob = AtlasHiveDatasetBlob(guid).get
      assert(version.getTags() == blob.tags)
      assert(version.getAttributes().get == blob.attributes)
    } finally {
      cleanup(f)
    }
  }
}
