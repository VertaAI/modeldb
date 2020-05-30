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

  /** Helper function to get current branch (or first branch alphabetically if there isn't one).  */
  def getGitBranchName(ref: String = "HEAD") = Try {
    val branches = Seq("git", "branch", "--points-at", ref).!!.trim().split("\n")
    val INDICATOR = "* "

    val curBranch = branches.filter(_ startsWith INDICATOR)

    if (!curBranch.isEmpty) curBranch.head.substring(INDICATOR.length) else branches.head
  }

  /** Helper function to retrieve git repository root directory */
  def getGitRepoRootDir() = Try {
    val dirPath = Seq("git", "rev-parse", "--show-toplevel").!!.trim()

    // Add trailing separator:
    if (dirPath.endsWith("/")) dirPath else dirPath + "/"
  }
}
