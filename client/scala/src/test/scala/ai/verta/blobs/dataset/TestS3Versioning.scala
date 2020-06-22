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

class TestS3Versioning extends FunSuite {
  def fixture =
    new {
      val testfilePath = "s3://verta-scala-test/testdir/testfile"
      val testfileLoc = S3Location(testfilePath).get

      val testfilePath2 = "s3://verta-scala-test/testdir/testsubdir/testfile2"
      val testfileLoc2 = S3Location(testfilePath2).get

      val testdirPath = "s3://verta-scala-test/testdir/"
      val testdirLoc = S3Location(testdirPath).get

      val testsubdirPath = "s3://verta-scala-test/testdir/testsubdir/"
      val testsubdirLoc = S3Location(testsubdirPath).get

      val bucketLoc = S3Location("s3://verta-scala-test").get
    }

  test("The downloaded s3 file should not be corrupted") {
    val f = fixture
    val s3Blob = S3(f.bucketLoc).get
    val s3PathToLocalMap = s3Blob.downloadFromS3().get

    val localPath = s3PathToLocalMap.get(f.testfilePath).get
    val pathBlob = PathBlob(localPath).get
    assert(pathBlob.getMetadata(localPath).get.md5 equals s3Blob.getMetadata(f.testfilePath).get.md5)

    val localPath2 = s3PathToLocalMap.get(f.testfilePath2).get
    val pathBlob2 = PathBlob(localPath2).get
    assert(pathBlob2.getMetadata(localPath2).get.md5 equals s3Blob.getMetadata(f.testfilePath2).get.md5)
  }
}
