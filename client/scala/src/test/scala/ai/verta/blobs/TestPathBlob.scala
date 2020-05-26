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


class TestPathBlob extends FunSuite {
  test("PathBlob should retrieve and store files in subdirs, but not the subdirs themselves") {
    val workingDir = System.getProperty("user.dir")
    val testDir = workingDir + "/src/test/scala/ai/verta/blobs/testdir"
    var path = PathBlob(List(testDir))

    assert(path.components.length == 2)

    var set = HashSet[String](
      path.components.head.path.get,
      path.components.tail.head.path.get
    )

    assert(set.contains(testDir + "/testfile"))
    assert(set.contains(testDir + "/testsubdir/testfile2"))
  }
}
