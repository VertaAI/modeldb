package ai.verta.blobs.dataset

import ai.verta.blobs._

import scala.language.reflectiveCalls

import org.scalatest.FunSuite
import org.scalatest.Assertions._
import scala.util.{Failure, Success, Try}

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

    val dirAttempt = pathBlob.getMetadata(f.testDir)
    val subdirAttempt = pathBlob.getMetadata(f.testDir)

    assert(dirAttempt.isFailure)
    assert(subdirAttempt.isFailure)
    assert(dirAttempt match {case Failure(e) => e.getMessage contains "is a directory"})
    assert(subdirAttempt match {case Failure(e) => e.getMessage contains "is a directory"})

    assert(pathBlob.getMetadata(f.testfile).isSuccess)
  }

  test("PathBlob should not store invalid paths, but should not stop execution") {
    val f = fixture
    val invalid = f.testDir + "/invalid-file"
    val pathBlob = PathBlob(List(invalid, f.testfile))


    val invalidAttempt = pathBlob.getMetadata(invalid)
    assert(invalidAttempt.isFailure)
    assert(invalidAttempt match {case Failure(e) => e.getMessage contains "No such file or directory"})
    assert(pathBlob.getMetadata(f.testfile).isSuccess)
  }

  test("PathBlob should not contain duplicate paths") {
    val f = fixture
    val pathBlob1 = PathBlob(List(f.testDir, f.testSubdir, f.testfile, f.testfile2, f.testfile))
    val pathBlob2 = PathBlob(List(f.testfile, f.testfile2))

    assert(pathBlob1 equals pathBlob2)
  }
}
