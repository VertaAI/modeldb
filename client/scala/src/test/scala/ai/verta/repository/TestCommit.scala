package ai.verta.repository

import ai.verta.client._
import ai.verta.blobs._
import ai.verta.blobs.dataset._

import scala.concurrent.ExecutionContext
import scala.language.reflectiveCalls
import scala.util.{Try, Success, Failure}

import java.io.File

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestCommit extends FunSuite {
  implicit val ec = ExecutionContext.global

  def fixture =
    new {
        val client = new Client(ClientConnection.fromEnvironment())
        val repo = client.getOrCreateRepository("My Repo").get
        val commit = repo.getCommitByBranch().get
        val pathBlob = PathBlob(f"${System.getProperty("user.dir")}/src/test/scala/ai/verta/blobs/testdir").get
        val s3Blob = S3(S3Location("s3://verta-scala-test/testdir/testsubdir/testfile2").get).get
    }

  def cleanup(
    f: AnyRef{val client: Client; val repo: Repository; val commit: Commit; val pathBlob: PathBlob; val s3Blob: S3}
  ) = {
    f.client.deleteRepository(f.repo.id)
    f.client.close()
  }

  test("Get should retrieve blobs that were updated") {
    val f = fixture

    try {
      val newCommit = f.commit.update("abc/def", f.pathBlob)
                              .flatMap(_.update("mnp/qrs", f.s3Blob)).get

      // check that the contents of the blobs are not corrupted:
      val pathBlob2 = newCommit.get("abc/def").get match {
        case blob: PathBlob => blob
        case blob: S3 => blob
      }
      assert(pathBlob2 equals f.pathBlob)

      val s3Blob2 = newCommit.get("mnp/qrs").get match {
        case blob: PathBlob => blob
        case blob: S3 => blob
      }
      assert(s3Blob2 equals f.s3Blob)
    } finally {
      cleanup(f)
    }
  }

  test("Get with invalid paths should fail") {
    val f = fixture

    try {
      val getAttempt = f.commit.get("xyz/tuv")
      assert(getAttempt.isFailure)
      assert(getAttempt match {case Failure(e) => e.getMessage contains "No blob was stored at this path."})
    } finally {
      cleanup(f)
    }
  }

  test("Getting pathblob with empty component should succeed") {
    val f = fixture
    val emptyFile = new File("emptyfile")

    try {
      emptyFile.createNewFile()
      val newCommit =
        f.commit.update("emptyfile", PathBlob("emptyfile").get).flatMap(_.save("save empty blob")).get
      assert(newCommit.get("emptyfile").isSuccess)
    } finally {
      emptyFile.delete()
      cleanup(f)
    }
  }

  test("Tagging unsaved commit should fail") {
    val f = fixture

    try {
      val newCommit = f.commit.update("abc/def", f.pathBlob).get
      val tagAttempt = newCommit.tag("Some tag")
      assert(tagAttempt.isFailure)
      assert(tagAttempt match {
        case Failure(e) => e.getMessage contains "Commit must be saved before it can be tagged"
      })
    } finally {
      cleanup(f)
    }
  }

  test("newBranch unsaved commit should fail") {
    val f = fixture

    try {
      val newCommit = f.commit.update("abc/def", f.pathBlob).get
      val newBranchAttempt = newCommit.newBranch("some-branch")
      assert(newBranchAttempt.isFailure)
      assert(newBranchAttempt match {
        case Failure(e) => e.getMessage contains "Commit must be saved before it can be attached to a branch"
      })
    } finally {
      cleanup(f)
    }
  }


  test("Saving unmodified commit should fail") {
    val f = fixture

    try {
      val newCommit = f.commit.update("abc/def", f.pathBlob)
                          .flatMap(_.save("Some msg")).get

      val saveAttempt = newCommit.save("Some message")
      assert(saveAttempt.isFailure)
      assert(saveAttempt match {
        case Failure(e) => e.getMessage contains "Commit is already saved"
      })
    } finally {
      cleanup(f)
    }
  }

  test("Saving a branched commit should update the branch's head") {
    val f = fixture

    try {
      val branchedCommit = f.commit.newBranch("some-branch").get
      val newCommit = branchedCommit.update("abc/def", f.pathBlob)
                                    .flatMap(_.save("Some msg"))
                                    .get

      // head of some-branch should be the new commit, not the old one
      assert(newCommit.equals(f.repo.getCommitByBranch("some-branch").get))
      assert(!branchedCommit.equals(f.repo.getCommitByBranch("some-branch")))
    } finally {
      cleanup(f)
    }
  }

  test("Saving should not corrupt the stored blobs") {
    val f = fixture

    try {
      val newId = f.commit.update("abc/def", f.pathBlob)
                          .flatMap(_.save("Some msg"))
                          .get.id.get

      val newCommit = f.repo.getCommitById(newId).get
      val getAttempt = newCommit.get("abc/def").get
      val pathBlob2 = getAttempt match {
        case blob: PathBlob => blob
      }
      assert(pathBlob2 equals f.pathBlob)
    } finally {
      cleanup(f)
    }
  }

  test("Multiple update and remove calls should be possible between savings") {
    val f = fixture

    try {
        val commit = f.commit.update("abc/cde", f.pathBlob)
                         .flatMap(_.save("Some message 1"))
                         .flatMap(_.update("def/ghi", f.s3Blob))
                         .flatMap(_.remove("abc/cde"))
                         .flatMap(_.save("Some message 2")).get

        assert(commit.get("abc/cde").isFailure)
        assert(commit.get("def/ghi").isSuccess)
    } finally {
      cleanup(f)
    }
  }

  test("Remove should discard blob from commit") {
    val f = fixture

    try {
      val newCommit = f.commit.update("abc/def", f.pathBlob)
                              .flatMap(_.update("mnp/qrs", f.pathBlob))
                              .flatMap(_.remove("abc/def")).get

      val getAttempt = newCommit.get("abc/def")
      assert(getAttempt.isFailure)
      assert(getAttempt match {case Failure(e) => e.getMessage contains "No blob was stored at this path."})
    } finally {
      cleanup(f)
    }
  }

  test("Remove a non-existing path should fail") {
    val f = fixture

    try {
      val removeAttempt = f.commit.remove("abc/def")
      assert(removeAttempt.isFailure)
      assert(removeAttempt match {case Failure(e) => e.getMessage contains "No blob was stored at this path."})
    } finally {
      cleanup(f)
    }
  }

  test("merge two commits with no conflicts") {
    val f = fixture

    try {
        val branch1 = f.repo.getCommitByBranch()
                       .flatMap(_.newBranch("a"))
                       .flatMap(_.update("abc/cde", f.pathBlob))
                       .flatMap(_.save("Some message 1")).get

        val branch2 = f.repo.getCommitByBranch()
                       .flatMap(_.newBranch("b"))
                       .flatMap(_.update("def/ghi", f.s3Blob))
                       .flatMap(_.save("Some message 2")).get

        val mergeAttempt = branch1.merge(branch2, message = Some("Merge test"))
        assert(mergeAttempt.isSuccess)

        val mergedCommit = mergeAttempt.get

        // check that the merging does not corrupt the commit:
        assert(mergedCommit.get("abc/cde").isSuccess)
        assert(mergedCommit.get("def/ghi").isSuccess)

        val retrievedS3Blob: S3 = mergedCommit.get("def/ghi").get match {
          case s3: S3 => s3
        }
        val retrievedPathBlob: PathBlob = mergedCommit.get("abc/cde").get match {
          case pathBlob: PathBlob => pathBlob
        }
        assert(retrievedS3Blob equals f.s3Blob)
        assert(retrievedPathBlob equals f.pathBlob)

        // check that the merging correctly assign the branch head:
        assert(f.repo.getCommitByBranch("a").get equals mergedCommit)
        assert(!(f.repo.getCommitByBranch("b").get equals mergedCommit))
        assert(f.repo.getCommitByBranch("b").get equals branch2)
    } finally {
      cleanup(f)
    }
  }

  test("merge conflicting branches should fail") {
    val f = fixture

    try {
        val branch1 = f.repo.getCommitByBranch()
                       .flatMap(_.newBranch("a"))
                       .flatMap(_.update("abc/cde", f.pathBlob))
                       .flatMap(_.save("Some message 1")).get

        // touch the file
        Thread.sleep(2000)
        var file = new File(f"${System.getProperty("user.dir")}/src/test/scala/ai/verta/blobs/testdir/testfile")
        file.setLastModified(System.currentTimeMillis())
        val newPathBlob = PathBlob(f"${System.getProperty("user.dir")}/src/test/scala/ai/verta/blobs/testdir").get
        val branch2 = f.repo.getCommitByBranch()
                       .flatMap(_.newBranch("b"))
                       .flatMap(_.update("abc/cde", newPathBlob))
                       .flatMap(_.save("Some message 2")).get

        val mergeAttempt = branch1.merge(branch2, message = Some("Merge test"))
        assert(mergeAttempt.isFailure)
        assert(mergeAttempt match {case Failure(e) => e.getMessage contains "Merge conflict"})
    } finally {
      cleanup(f)
    }
  }

  test("merge unsaved commits should fail") {
    val f = fixture

    try {
      val branch1 = f.repo.getCommitByBranch()
                     .flatMap(_.newBranch("a"))
                     .flatMap(_.update("abc/cde", f.pathBlob))
                     .flatMap(_.save("Some message 1")).get

      val branch2 = f.repo.getCommitByBranch()
                     .flatMap(_.newBranch("b"))
                     .flatMap(_.update("def/ghi", f.s3Blob)).get

        val mergeAttempt = branch1.merge(branch2, message = Some("Merge test"))
        assert(mergeAttempt.isFailure)
        assert(mergeAttempt match {case Failure(e) => e.getMessage contains "Other commit must be saved"})

        val mergeAttempt2 = branch2.merge(branch1, message = Some("Merge test"))
        assert(mergeAttempt2.isFailure)
        assert(mergeAttempt2 match {case Failure(e) => e.getMessage contains "This commit must be saved"})
    } finally {
      cleanup(f)
    }
  }

  test("merge commits from different repository should fail") {
    val f = fixture
    val newRepoAttempt = f.client.getOrCreateRepository("My Repo 2")

    try {
      val repo2 = newRepoAttempt.get

      val branch1 = repo2.getCommitByBranch()
                     .flatMap(_.newBranch("a"))
                     .flatMap(_.update("abc/cde", f.pathBlob))
                     .flatMap(_.save("Some message 1")).get

      val branch2 = f.repo.getCommitByBranch()
                     .flatMap(_.newBranch("b"))
                     .flatMap(_.update("def/ghi", f.s3Blob))
                     .flatMap(_.save("Some message 2")).get

      val mergeAttempt = branch1.merge(branch2, message = Some("Merge test"))
      assert(mergeAttempt.isFailure)
      assert(mergeAttempt match {case Failure(e) => e.getMessage contains "Two commits must belong to the same repository"})

    } finally {
      newRepoAttempt.flatMap(repo => f.client.deleteRepository(repo.id))
      cleanup(f)
    }
  }

  test("commit's log should return the right commits in the right order") {
    val f = fixture

    try {
      // parent1 - parent2 - parent3---desc
      //         - parent4  ----------/

      val parent1 = f.commit
      val parent2 = parent1.update("abc/def", f.pathBlob).flatMap(_.save("some message 2")).get
      val parent3 = parent2.update("ghi/jkl", f.pathBlob).flatMap(_.save("some message 3")).get
      val parent4 = parent1.newBranch("new-branch")
                           .flatMap(_.update("uvw/wer", f.pathBlob))
                           .flatMap(_.save("some message 4")).get
      val desc = parent3.merge(parent4).get
      val descDirty = desc.update("some-path", f.pathBlob).get

      assert(desc.log().get == Stream(desc, parent4, parent3, parent2, parent1))
      assert(descDirty.log().get == desc.log().get)
    } finally {
      cleanup(f)
    }
  }

  test ("diffFrom and applyDiff of 2 different branches should modify one branch to match the other") {
    val f = fixture

    try {
        val originalCommit = f.commit.update("to-remove", f.pathBlob)
                              .flatMap(_.update("to-update", f.s3Blob))
                              .flatMap(_.save("original commit")).get

        val branch1 = originalCommit.newBranch("a")
                                    .flatMap(_.update("abc/cde", f.pathBlob))
                                    .flatMap(_.save("Some message 11"))
                                    .flatMap(_.update("wuv/ajf", f.pathBlob))
                                    .flatMap(_.save("Some message 12")).get

        val branch2 = originalCommit.newBranch("b")
                                    .flatMap(_.update("abc/cde", f.s3Blob))
                                    .flatMap(_.remove("to-remove"))
                                    .flatMap(_.save("Some message 21"))
                                    .flatMap(_.update("def/ghi", f.pathBlob))
                                    .flatMap(_.update("to-update", f.pathBlob))
                                    .flatMap(_.save("Some message 22")).get

        val diff = branch2.diffFrom(Some(branch1)).get

        val newBranch1 = branch1.applyDiff(diff, "apply diff").get
        assert(newBranch1.get("wuv/ajf").isFailure)
        assert(newBranch1.get("def/ghi").isSuccess)
        assert(newBranch1.get("to-remove").isFailure)

        val retrievedS3Blob: S3 = newBranch1.get("abc/cde").get match {
          case s3: S3 => s3
        }
        assert(retrievedS3Blob equals f.s3Blob)

        val retrievedPathBlob: PathBlob = branch2.get("to-update").get match {
          case pathBlob: PathBlob => pathBlob
        }
        assert(retrievedPathBlob equals f.pathBlob)

        // check the branching behavior:
        assert(f.repo.getCommitByBranch("a").get equals newBranch1)
        assert(f.repo.getCommitByBranch("b").get equals branch2)
    } finally {
      cleanup(f)
    }
  }

  test("diffFrom with no commit passed should compute diff with parent") {
    val f = fixture

    try {
        val commit = f.commit.update("abc", f.pathBlob)
                      .flatMap(_.update("def", f.s3Blob))
                      .flatMap(_.save("original commit")).get

        val diff = commit.diffFrom().get
        val newCommit = f.commit.newBranch("new-branch")
                         .flatMap(_.applyDiff(diff, "apply diff")).get

        assert(newCommit.get("abc").isSuccess)
        assert(newCommit.get("def").isSuccess)
        assert(f.repo.getCommitByBranch("new-branch").get equals newCommit)
    } finally {
      cleanup(f)
    }
  }

  test("applyDiff should assign the corrent parent to the new commit") {
    val f = fixture

    try {
      val branchA = f.commit.newBranch("branch-a")
                            .flatMap(_.update("some-path-1", f.pathBlob))
                            .flatMap(_.update("some-path-2", f.pathBlob))
                            .flatMap(_.save("add the blobs")).get

      val branchB = f.commit.newBranch("branch-b")
                            .flatMap(_.update("some-path-3", f.pathBlob))
                            .flatMap(_.update("some-path-4", f.pathBlob))
                            .flatMap(_.save("add the blobs")).get

      val newB = branchB.applyDiff(branchA.diffFrom(Some(branchB)).get, "diff").get
      val log = newB.log().get.toList
      assert(log equals List(newB, branchB, f.commit))
    } finally {
      cleanup(f)
    }
  }

  test("revert with no commit passed should revert the latest commit") {
    val f = fixture

    try {
        val preRevert = f.commit.update("abc/cde", f.pathBlob)
                         .flatMap(_.save("Some message 1"))
                         .flatMap(_.update("def/ghi", f.s3Blob))
                         .flatMap(_.remove("abc/cde"))
                         .flatMap(_.save("Some message 2")).get

        val revCommit = preRevert.revert(message = Some("Revert test")).get

        assert(revCommit.get("abc/cde").isSuccess)
        assert(revCommit.get("def/ghi").isFailure)
    } finally {
      cleanup(f)
    }
  }

  test("revert with another commit passed should revert the changes from that commit") {
    val f = fixture

    try {
        val firstCommit = f.commit.update("abc/cde", f.pathBlob)
                           .flatMap(_.update("mnp/qrs", f.pathBlob))
                           .flatMap(_.save("Some message 1")).get

        val secondCommit = firstCommit.update("def/ghi", f.s3Blob)
                                      .flatMap(_.update("tuv/wxy", f.pathBlob))
                                      .flatMap(_.save("Some message 2")).get

        val thirdCommit = secondCommit.remove("abc/cde")
                                      .flatMap(_.save("Some message 3")).get

        val revCommit = thirdCommit.revert(secondCommit, Some("Revert test")).get

        // first and third commit should be retained:
        assert(revCommit.get("abc/cde").isFailure)
        assert(revCommit.get("mnp/qrs").isSuccess)
        // second commit should be reverted:
        assert(revCommit.get("def/ghi").isFailure)
        assert(revCommit.get("tuv/wxy").isFailure)

        // log should contain the correct commits:
        assert(revCommit.log().get.toList equals List(revCommit, thirdCommit, secondCommit, firstCommit, f.commit))
      } finally {
        cleanup(f)
      }
    }

    test("revert unsaved commit should fail") {
      val f = fixture

      try {
          val firstCommit = f.commit.newBranch("new-branch-1")
                             .flatMap(_.update("abc/cde", f.pathBlob))
                             .flatMap(_.save("Some message 1")).get

          val secondCommit = f.commit.newBranch("new-branch-2")
                                     .flatMap(_.update("def/ghi", f.s3Blob)).get

          val revAttempt = firstCommit.revert(secondCommit, Some("Revert test"))
          assert(revAttempt.isFailure)
          assert(revAttempt match {case Failure(e) => e.getMessage contains "Other commit must be saved"})

          val revAttempt2 = secondCommit.revert(firstCommit, Some("Revert test"))
          assert(revAttempt2.isFailure)
          assert(revAttempt2 match {case Failure(e) => e.getMessage contains "This commit must be saved"})

      } finally {
        cleanup(f)
      }
    }

    test("revert a merge commit should remove changes in the other branch") {
      val f = fixture

      try {
        val originalCommit = f.commit.update("a", f.pathBlob)
                              .flatMap(_.save("Some message")).get

        val firstCommit = originalCommit.newBranch("new-branch-1")
                             .flatMap(_.update("b", f.pathBlob))
                             .flatMap(_.save("Some message 1")).get

        val secondCommit = originalCommit.newBranch("new-branch-2")
                                         .flatMap(_.remove("a"))
                                         .flatMap(_.update("def/ghi", f.s3Blob))
                                         .flatMap(_.save("Some message 2")).get

        val mergeCommit = firstCommit.merge(secondCommit, Some("Merge commits")).get
        val revCommit = mergeCommit.revert().get

        assert(revCommit.get("a").isSuccess)
        assert(revCommit.get("def/ghi").isFailure)
        assert(revCommit.get("b").isSuccess)
      } finally {
        cleanup(f)
      }
    }

    test("revert twice should undo the first revert") {
      val f = fixture

      try {
        val originalCommit = f.commit.update("a", f.pathBlob)
                              .flatMap(_.save("Some message")).get

        val updateCommit = originalCommit.update("b", f.pathBlob)
                                         .flatMap(_.remove("a"))
                                         .flatMap(_.save("Some message 1")).get

        val revertCommit = updateCommit.revert().flatMap(_.revert()).get
        assert(revertCommit.get("a").isFailure)
        assert(revertCommit.get("b").isSuccess)
      } finally {
        cleanup(f)
      }
    }

    test("revert applyDiff should undo the the changes") {
      val f = fixture

      try {
        val branchA = f.commit.newBranch("branch-a")
                              .flatMap(_.update("some-path-1", f.pathBlob))
                              .flatMap(_.save("add the blobs")).get

        val branchB = f.commit.newBranch("branch-b")
                              .flatMap(_.update("some-path-2", f.pathBlob))
                              .flatMap(_.save("add the blobs")).get

        val newB = branchB.applyDiff(branchA.diffFrom(Some(branchB)).get, "diff").get
        val revertCommit = newB.revert().get

        // changes induced by applyDiff should be undone
        assert(revertCommit.get("some-path-2").isSuccess)
        assert(revertCommit.get("some-path-1").isFailure)

        // correct log:
        val log = revertCommit.log().get.toList
        assert(log equals List(revertCommit, newB, branchB, f.commit))
      } finally {
        cleanup(f)
      }
    }

    test("revert should affect the correct branch") {
      val f = fixture

      try {
        val originalCommit = f.commit.update("a", f.pathBlob)
                              .flatMap(_.save("Some message"))
                              .flatMap(_.update("b", f.pathBlob))
                              .flatMap(_.save("Some message 1")).get


        val newBranch = originalCommit.newBranch("new-branch").get
        val revertCommit = newBranch.revert().get

        assert(f.repo.getCommitByBranch().get equals originalCommit) // master shouldn't be affect by revert
        assert(f.repo.getCommitByBranch("new-branch").get equals revertCommit)
      } finally {
        cleanup(f)
      }
    }

    test("walk should visit all the blobs and folders in the correct order") {
      val f = fixture

      try {
        val newCommit = f.commit.update("file1", f.pathBlob)
                                .flatMap(_.update("a/file2", f.pathBlob))
                                .flatMap(_.update("a/file3", f.pathBlob))
                                .flatMap(_.update("a/b/file4", f.pathBlob))
                                .flatMap(_.update("a/c/file5", f.pathBlob))
                                .flatMap(_.save("walkzzz")).get
        val allBlobs = newCommit.walk(CommitLister()).map(_.get).filter(_.isDefined).toList.map(_.get)
        assert(allBlobs equals List("file1", "a/file2", "a/file3", "a/b/file4", "a/c/file5"))

        val numFolders = newCommit.walk(FolderCounter()).map(_.get).sum
        assert(numFolders equals 3)

        val numBlobs = newCommit.walk(BlobCounter()).map(_.get).sum
        assert(numBlobs equals 5)
      } finally {
        cleanup(f)
      }
    }

    test("filtering folder should skip the their subtree") {
      val f = fixture

      try {
        val newCommit = f.commit.update("file1", f.pathBlob)
                                .flatMap(_.update("a/file2", f.pathBlob))
                                .flatMap(_.update("a/file3", f.pathBlob))
                                .flatMap(_.update("a/b/file4", f.pathBlob))
                                .flatMap(_.update("a/c/file5", f.pathBlob))
                                .flatMap(_.save("walkzzz")).get

       val blobs = newCommit.walk(FilterWalker()).map(_.get).filter(_.isDefined).toList.map(_.get)
       assert(blobs equals List("file1", "a/file2", "a/file3", "a/c/file5"))

      } finally {
        cleanup(f)
      }
    }

    test("walk on empty commit should produce an empty stream") {
      val f = fixture

      try {
        assert(f.commit.walk(FolderCounter()).isEmpty)
      } finally {
        cleanup(f)
      }
    }
}
