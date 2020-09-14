package ai.verta.blobs.dataset

import ai.verta.blobs._

import scala.language.reflectiveCalls
import scala.util.{Failure, Success, Try}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

import java.io.FileNotFoundException

class TestAtlasHiveDatasetBlob extends FunSuite {
  test("Atlas hive blob should save the correct query and connection") {
    val guid: String = ???
    val expectedNumRecords: BigInt = ???
    val expectedDatabaseName: String = ???
    val expectedTableName: String = ???
    val expectedQuery: String = f"select * from ${expectedDatabaseName}.${expectedTableName}"

    val atlasHiveBlob = AtlasHiveDatasetBlob(guid)
    assert(atlasHiveBlob.query.get == expectedQuery)
    assert(atlasHiveBlob.numRecords.get == expectedNumRecords)
  }
}
