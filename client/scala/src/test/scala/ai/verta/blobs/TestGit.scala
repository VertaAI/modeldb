package ai.verta.blobs

import org.scalatest.FunSuite
import org.scalatest.Assertions._

import scala.util.{Try, Success, Failure}

class TestGit extends FunSuite {
  test("get remote") {
    assert(Git.getGitRemoteURL().get contains "modeldb.git")
  }

  test("get branch") {
    assert(Git.getGitBranchName().get.length > 0)
  }

  test("get root directory of git repository") {
    assert(Git.getGitRepoRootDir().get.length > 0)
  }

  test("get commit hash") {
    assert(Git.getGitCommitHash().get.length > 0)
  }

  test("get commit dirtiness") {
    val dirtiness = Git.getGitCommitDirtiness()
    assert(dirtiness.isInstanceOf[Success[Boolean]])
  }
}
