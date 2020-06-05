package ai.verta.blobs.dataset

import ai.verta.blobs._

import scala.language.reflectiveCalls

import org.scalatest.FunSuite
import org.scalatest.Assertions._

import java.io.FileNotFoundException

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
    var pathBlob = PathBlob(List(f.testfile)).get

    assertMetadata(pathBlob.getMetadata(f.testfile).get, f.testfile)
  }

  test("PathBlob should retrieve multiple files correctly") {
    val f = fixture
    var pathBlob = PathBlob(List(f.testfile, f.testfile2)).get

    assertMetadata(pathBlob.getMetadata(f.testfile).get, f.testfile)
    assertMetadata(pathBlob.getMetadata(f.testfile2).get, f.testfile2)
  }

  test("PathBlob should not store directory") {
    val f = fixture
    val pathBlob = PathBlob(List(f.testDir, f.testSubdir)).get

    val dirAttempt = pathBlob.getMetadata(f.testDir)
    val subdirAttempt = pathBlob.getMetadata(f.testDir)

    assert(dirAttempt.isEmpty)
    assert(subdirAttempt.isEmpty)
    assert(pathBlob.getMetadata(f.testfile).isDefined)
  }

  test("PathBlob constructor should fail when an invalid path is passed") {
    val f = fixture
    val invalid = f.testDir + "/invalid-file"
    assert(PathBlob(List(invalid, f.testfile)).isFailure)
  }

  test("PathBlob should not contain duplicate paths") {
    val f = fixture
    val pathBlob1 = PathBlob(List(f.testDir, f.testSubdir, f.testfile, f.testfile2, f.testfile)).get
    val pathBlob2 = PathBlob(List(f.testfile, f.testfile2)).get

    assert(pathBlob1 equals pathBlob2)
  }
}
