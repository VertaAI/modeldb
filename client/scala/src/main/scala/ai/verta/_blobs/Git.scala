package ai.verta._blobs

import ai.verta.swagger._public.modeldb.versioning.model._

/** Captures metadata about the git commit with the specified `branch`, `tag`, or `commit_hash`
 *  TODO: implement autocapture behavior
 */
case class Git(
  val branch: Option[String] = None, val hash: Option[String] = None,
  val isDirty: Option[Boolean], val tag: Option[String] = None, val repo: Option[String] = None
) extends Code {
  // Basically a wrapper for VersioningGitCodeBlob

  val versioningGitCodeBlob = VersioningGitCodeBlob(
    branch = branch, hash = hash, is_dirty = isDirty, tag = tag, repo = repo
  )
  
  val versioningBlob = VersioningBlob(
      code = Some(VersioningCodeBlob(git = Some(versioningGitCodeBlob)))
  )
}
