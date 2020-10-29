package ai.verta.blobs.dataset

import ai.verta.client._
import ai.verta.blobs._
import ai.verta.swagger.client.HttpException
import ai.verta.swagger._public.modeldb.versioning.model.VersioningSetTagRequestResponse

import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._
import collection.JavaConverters._

import scala.collection.mutable.HashSet

class TestVersioning extends FunSuite {
  def fixture =
    new {
      val testfilePath = "s3://verta-starter/census-train.csv"
      val testfileLoc = S3Location(testfilePath).get

      val testfilePath2 = "s3://verta-starter/census-test.csv"
      val testfileLoc2 = S3Location(testfilePath2).get

      val workingDir = System.getProperty("user.dir")
      val testDir = workingDir + "/src/test/scala/ai/verta/blobs/testdir"
      val testfile = testDir + "/testfile"
      val testSubdir = testDir + "/testsubdir"
      val testfile2 = testSubdir + "/testfile2"
    }

  test("Combining a Dataset blob that enables versioning with one that doesn't should fail") {
    val f = fixture
    val s3Blob = S3(f.testfileLoc, true).get
    val s3Blob2 = S3(f.testfileLoc2, false).get

    val combineAttempt = S3.reduce(s3Blob, s3Blob2)
    assert(combineAttempt.isFailure)
    assert(combineAttempt match {
      case Failure(e) => e.getMessage contains "Cannot combine a blob that enables versioning with a blob that does not"
    })

    val pathBlob = PathBlob(f.testfile, true).get
    val pathBlob2 = PathBlob(f.testfile2, false).get

    val combineAttempt2 = PathBlob.reduce(pathBlob, pathBlob2)
    assert(combineAttempt2.isFailure)
    assert(combineAttempt2 match {
      case Failure(e) => e.getMessage contains "Cannot combine a blob that enables versioning with a blob that does not"
    })
  }
}
