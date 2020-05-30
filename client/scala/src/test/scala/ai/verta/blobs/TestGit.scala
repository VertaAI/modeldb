package ai.verta.blobs

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestGit extends FunSuite {
  test("get remote") {
    assert(getGitRemoteURL() contains "modeldb.git")
  }

  test("get branch") {
    println(getGitBranchName())
  }
}
