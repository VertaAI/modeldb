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
    val guid = sys.env.get("GUID").get
    val expectedNumRecords: BigInt = 8279779
    val expectedDatabaseName = "default"
    val expectedTableName = "trip_details_by_zone"
    val expectedType = "hive_table"
    val expectedQuery = f"select * from ${expectedDatabaseName}.${expectedTableName}"

    val atlasHiveBlob = AtlasHiveDatasetBlob(guid).get

    assert(atlasHiveBlob.query.get == expectedQuery)
    assert(atlasHiveBlob.numRecords.get == expectedNumRecords)

    val attributes = atlasHiveBlob.attributes
    assert(attributes.get("table_name").get.asString.get == expectedTableName)
    assert(attributes.get("database_name").get.asString.get == expectedDatabaseName)
    assert(attributes.get("type").get.asString.get == expectedType)
    assert(attributes.get("created_time").isDefined)
    assert(attributes.get("updated_time").isDefined)
  }
}
