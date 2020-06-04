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

class TestPathBlob extends FunSuite {
  /** Verify that a FileMetadata has correct path and does not have invalid parameter
   *  @param metadata file's metadata
   *  @param path file's path (to be checked)
   */
  def assertMetadata(metadata: FileMetadata, path: String) = {
    assert(metadata.path.equals(path))
    assert(metadata.size > 0)
    assert(metadata.lastModified > 0)
    assert(metadata.md5.length > 0)
  }

  def fixture =
    new {
      val workingDir = System.getProperty("user.dir")
      val testDir = workingDir + "/src/test/scala/ai/verta/blobs/testdir"
      val testfile = testDir + "/testfile"
      val testSubdir = testDir + "/testsubdir"
      val testfile2 = testSubdir + "/testfile2"
    }

  test("PathBlob should retrieve a file's metadata correctly") {
    val f = fixture
    var pathBlob = PathBlob(List(f.testfile))

    assertMetadata(pathBlob.getMetadata(f.testfile).get, f.testfile)
  }

  test("PathBlob should retrieve multiple files correctly") {
    val f = fixture
    var pathBlob = PathBlob(List(f.testfile, f.testfile2))

    assertMetadata(pathBlob.getMetadata(f.testfile).get, f.testfile)
    assertMetadata(pathBlob.getMetadata(f.testfile2).get, f.testfile2)
  }

  test("PathBlob should not store directory") {
    val f = fixture
    val pathBlob = PathBlob(List(f.testDir, f.testSubdir))

    assert(pathBlob.getMetadata(f.testDir).isEmpty)
    assert(pathBlob.getMetadata(f.testSubdir).isEmpty)
    assert(pathBlob.getMetadata(f.testfile).isDefined)
  }

  test("PathBlob should not store invalid paths, but should not stop execution") {
    val f = fixture
    val invalid = f.testDir + "/invalid-file"
    val pathBlob = PathBlob(List(invalid, f.testfile))

    assert(pathBlob.getMetadata(invalid).isEmpty)
    assert(pathBlob.getMetadata(f.testfile).isDefined)
  }

  test("PathBlob should not contain duplicate paths") {
    val f = fixture
    val pathBlob1 = PathBlob(List(f.testDir, f.testSubdir, f.testfile, f.testfile2, f.testfile))
    val pathBlob2 = PathBlob(List(f.testfile, f.testfile2))

    assert(pathBlob1 equals pathBlob2)
  }
}
