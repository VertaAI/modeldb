package ai.verta

import ai.verta.swagger._public.modeldb.versioning.model._

import scala.util.{Failure, Success, Try}
import scala.sys.process._

package object blobs {
  /** Helper function to convert a VersioningBlob instance to corresponding Blob subclass instance
   *  @param vb the VersioningBlob instance
   *  @return an instance of a Blob subclass
   *  TODO: finish the pattern matching with other blob subclasses
   */
  def versioningBlobToBlob(vb: VersioningBlob): Blob = vb match {
    case VersioningBlob(Some(VersioningCodeBlob(Some(git), _)), _, _, _) => new Git(git)

    case VersioningBlob(_, _, Some(VersioningDatasetBlob(Some(path), _)), _) => new PathBlob(path)

    case VersioningBlob(_, _, Some(VersioningDatasetBlob(_, Some(s3))), _) => new S3(s3)

    case _ => Git()
  }


  /** Analogous to os.path.expanduser
   *  From https://stackoverflow.com/questions/6803913/java-analogous-to-python-os-path-expanduser-os-path-expandvars
   *  @param path path
   *  @return path, but with (first occurence of) ~ replace with user's home directory
   */
  def expanduser(path: String) = path.replaceFirst("~", System.getProperty("user.home"))

  // def autocaptureGit() = {
  //
  // }

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
