package ai.verta.blobs

import ai.verta.swagger._public.modeldb.versioning.model._

/** Captures metadata about the git commit with the specified `branch`, `tag`, or `commit_hash`
 *  TODO: implement autocapture behavior
 */
case class Git(
  val branch: Option[String] = None, val hash: Option[String] = None,
  val isDirty: Option[Boolean] = None, val tag: Option[String] = None,
  val repo: Option[String] = None
) extends Code {
  // Basically a wrapper for VersioningGitCodeBlob

  /*  Auxiliary constructor for conversion from VersioningBlob instance  */
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
