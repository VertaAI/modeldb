package ai.verta.blobs

import ai.verta.swagger._public.modeldb.versioning.model._

import scala.sys.process._
import scala.util.{Failure, Success, Try}

/** Captures metadata about the git commit with the specified `branch`, `tag`, or `commit_hash`
 *  TODO: implement autocapture behavior
 */
case class Git(
  val branch: Option[String] = None, val hash: Option[String] = None,
  val isDirty: Option[Boolean] = None, val tag: Option[String] = None,
  val repo: Option[String] = None
) extends Code {
  // Basically a wrapper for VersioningGitCodeBlob

  /**  Auxiliary constructor for conversion from VersioningBlob instance  */
  def this(gitBlob: VersioningGitCodeBlob) {
    this(gitBlob.branch, gitBlob.hash, gitBlob.is_dirty, gitBlob.tag, gitBlob.repo)
  }

  var versioningGitCodeBlob = VersioningGitCodeBlob(
    branch = branch, hash = hash, is_dirty = isDirty, tag = tag, repo = repo
  )

  var versioningBlob = VersioningBlob(
      code = Some(VersioningCodeBlob(git = Some(versioningGitCodeBlob)))
  )
}

object Git {
  // Companion object for Git

  /** Helper function to get remote repository url */
  def getGitRemoteURL() = Try(Seq("git", "ls-remote", "--get-url").!!.trim())

  /** Helper function to get current commit's hash */
  def getGitCommitHash(ref: String = "HEAD") =
    Try(Seq("git", "rev-parse", f"${ref}^{commit}").!!.trim()) orElse
    Try(Seq("git", "rev-parse", "--verify", ref).!!.trim())

  /** Helper function to get current branch (or first branch alphabetically if there isn't one).  */
  def getGitBranchName(ref: String = "HEAD") = Try {
    val branches = Seq("git", "branch", "--points-at", ref).!!.trim().split("\n")
    val INDICATOR = "* "

    val curBranch = branches.filter(_ startsWith INDICATOR)

    if (!curBranch.isEmpty) curBranch.head.substring(INDICATOR.length) else branches.head
  }

  /** Helper function to retrieve tag of commit */
  def getGitCommitTag(ref: String = "HEAD"): Try[Option[String]] = Try {
    val tags = Seq("git", "tag", "--points-at", ref).!!.trim().split("\n")
    if (tags.isEmpty) None else Some(tags.head)
  }

  /** Helper function to retrieve git repository root directory */
  def getGitRepoRootDir() = Try {
    val dirPath = Seq("git", "rev-parse", "--show-toplevel").!!.trim()

    // Add trailing separator:
    if (dirPath.endsWith("/")) dirPath else dirPath + "/"
  }

  /** Helper function to check if a commit is dirty */
  def getGitCommitDirtiness(ref: Option[String] = None) = ref match {
    case None => Try {
      !Seq("git", "status", "--porcelain").!!.split("\n").forall(_ startsWith "??")
    }
    case Some(_) => Try { // compare `ref` to the working tree and index
      Seq("git", "diff-index", ref.get).!!.split("\n").length > 0
    }
  }
}
