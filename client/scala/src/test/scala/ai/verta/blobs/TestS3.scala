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
}
