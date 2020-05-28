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
  test("PathBlob should retrieve a file's metadata correctly") {
    val workingDir = System.getProperty("user.dir")
    val testDir = workingDir + "/src/test/scala/ai/verta/blobs/testdir/testfile"
    var path = PathBlob(List(testDir))

    assert(path.components.length == 1)

    val component = path.components.head

    assert(component.path.get.equals(testDir))
    assert(component.md5.get != "")
    assert(component.size.get > 0)
    assert(component.last_modified_at_source.get > 0)
  }

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

  test("PathBlob should not contain duplicate paths") {
    val workingDir = System.getProperty("user.dir")
    val testDir = workingDir + "/src/test/scala/ai/verta/blobs/testdir"
    val testDir2 = testDir + "/testfile"
    val testDir3 = testDir + "/testsubdir/testfile2"
    val testDir4 = testDir + "/testsubdir/"

    var path = PathBlob(List(testDir, testDir2, testDir3, testDir4))

    assert(path.components.length == 2)

    var set = HashSet[String](
      path.components.head.path.get,
      path.components.tail.head.path.get
    )

    assert(set.contains(testDir2))
    assert(set.contains(testDir3))
  }
}
