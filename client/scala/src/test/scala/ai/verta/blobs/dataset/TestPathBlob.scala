package ai.verta.blobs.dataset

import ai.verta.blobs._

import scala.language.reflectiveCalls
import scala.util.{Failure, Success, Try}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

import java.io.FileNotFoundException

class TestPathBlob extends FunSuite {
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
    var pathBlob = PathBlob(f.testfile).get

    TestMetadata.assertMetadata(pathBlob.getMetadata(f.testfile).get, f.testfile)
    assert(pathBlob.listPaths equals List(f.testfile))
  }

  test("PathBlob should retrieve multiple files correctly") {
    val f = fixture
    var pathBlob = PathBlob(List(f.testfile, f.testfile2)).get

    TestMetadata.assertMetadata(pathBlob.getMetadata(f.testfile).get, f.testfile)
    TestMetadata.assertMetadata(pathBlob.getMetadata(f.testfile2).get, f.testfile2)
    assert(pathBlob.listPaths.toSet equals Set(f.testfile, f.testfile2))
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
    val invalidAttempt = PathBlob(List(invalid, f.testfile))

    assert(invalidAttempt.isFailure)
    assert(invalidAttempt match {case Failure(e) => e.getMessage contains "No such file or directory"})
  }

  test("PathBlob should not contain duplicate paths") {
    val f = fixture
    val pathBlob1 = PathBlob(List(f.testDir, f.testSubdir, f.testfile, f.testfile2, f.testfile)).get
    val pathBlob2 = PathBlob(List(f.testfile, f.testfile2)).get

    assert(pathBlob1 equals pathBlob2)
    assert(pathBlob1.listPaths.toSet equals Set(f.testfile, f.testfile2))
  }

  test("Reducing PathBlobs should retain the contents of both") {
    val f = fixture
    val pathBlob = PathBlob(List(f.testfile, f.testfile2)).get
    val pathBlobCombined = PathBlob.reduce(
      PathBlob(f.testfile).get,
      PathBlob(f.testSubdir).get
    ).get

    assert(pathBlob equals pathBlobCombined)
    assert(pathBlobCombined.listPaths.toSet equals Set(f.testfile, f.testfile2))
  }
}
