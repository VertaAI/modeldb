package ai.verta.blobs.dataset

import ai.verta.blobs._

import scala.language.reflectiveCalls
import scala.util.{Failure, Success, Try}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

import java.io.FileNotFoundException

class TestQueryDatasetBlob extends FunSuite {
  def fixture =
    new {
      val query = "SELECT * FROM ner-table"
      val dbConnectionStr = "localhost:6543"
      val numRecords = 100
      val rdbmsBlob = RDBMSDatasetBlob(query, dbConnectionStr, Some(numRecords))
    }

  test("RDBMS blob should save the correct query and connection") {
    val f = fixture
    assert(f.rdbmsBlob.query.get == f.query)
    assert(f.rdbmsBlob.dataSourceURI.get == f.dbConnectionStr)
    assert(f.rdbmsBlob.numRecords.get == f.numRecords)
  }
}
