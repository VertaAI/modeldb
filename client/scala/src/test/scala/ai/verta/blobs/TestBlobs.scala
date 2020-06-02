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


class TestBlobs extends FunSuite {
  test("conversion between VersioningBlob and Blob should maintain blob type") {
    assert(versioningBlobToBlob(PathBlob(List()).versioningBlob).isInstanceOf[PathBlob])
    assert(versioningBlobToBlob(S3(List()).versioningBlob).isInstanceOf[S3])
    assert(versioningBlobToBlob(Git().versioningBlob).isInstanceOf[Git])
  }
}
