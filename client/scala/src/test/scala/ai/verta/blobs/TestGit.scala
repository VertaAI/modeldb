package ai.verta.blobs

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestGit extends FunSuite {
  test("get remote") {
    assert(getGitRemoteURL().get contains "modeldb.git")
  }

  test("get branch") {
    assert(getGitBranchName().get.length > 0)
  }

  test("get root directory of git repository") {
    assert(getGitRepoRootDir().get.length > 0)
  }
}
