package ai.verta.blobs.dataset

import ai.verta.blobs._

import scala.language.reflectiveCalls
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

import java.io.FileNotFoundException

class TestAtlasHiveDatasetBlob extends FunSuite {
  implicit val ec = ExecutionContext.global

  test("Atlas hive blob should save the correct query and connection") {
    val guid: String = sys.env.get("GUID").get
    val expectedNumRecords: BigInt = 8279779
    val expectedDatabaseName: String = "default"
    val expectedTableName: String = "trip_details_by_zone"
    val expectedQuery: String = f"select * from ${expectedDatabaseName}.${expectedTableName}"

    val atlasHiveBlob = AtlasHiveDatasetBlob(guid).get
    assert(atlasHiveBlob.query.get == expectedQuery)
    assert(atlasHiveBlob.numRecords.get == expectedNumRecords)
  }
}
