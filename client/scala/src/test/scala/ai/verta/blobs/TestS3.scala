package ai.verta.blobs


import ai.verta.client._
import ai.verta.blobs._
import ai.verta.swagger.client.HttpException
import ai.verta.swagger._public.modeldb.versioning.model.VersioningSetTagRequestResponse

import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

import scala.collection.mutable.HashSet


class TestS3 extends FunSuite {
  test("S3Location should correctly determine bucket name and key") {
    val s31 = new S3Location("s3://verta-starter/census-train.csv")

    assert(s31.bucketName.equals("verta-starter"))
    assert(s31.key.get.equals("census-train.csv"))

    val s32 = new S3Location("s3://verta-starter")
    assert(s32.bucketName.equals("verta-starter"))
    assert(s32.key.isEmpty)
  }

  test("S3Location should throw an exception when encountered non-S3 path") {
    assertThrows[IllegalArgumentException] {
      val s3 = new S3Location("http://verta-starter/census-train.csv")
    }
  }

  test("S3 blob should retrieve the file correctly") {
    val s3Loc = new S3Location("s3://verta-scala-test/testdir/testfile")
    val s3 = S3(List(s3Loc))

    assert(s3.components.length == 1)

    val s3File = s3.components.head.path.get
    assert(s3File.path.get.equals("s3://verta-scala-test/testdir/testfile"))
    assert(s3File.size.get > 0)
    assert(s3File.last_modified_at_source.get > 0)
    assert(s3File.md5.get.length > 0)
  }
}
