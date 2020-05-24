package ai.verta

import ai.verta.swagger._public.modeldb.versioning.model._

package object blobs {
  /** Helper function to convert a VersioningBlob instance to corresponding Blob subclass instance
   *  TODO: implement
   */
  def versioningBlobToBlob(vb: VersioningBlob): Blob = vb match {
    case VersioningBlob(
      Some(VersioningCodeBlob(Some(VersioningGitCodeBlob(branch, hash, is_dirty, repo, tag)), None)),
      None, None, None) => Git(branch, hash, is_dirty, repo, tag)
  }
}
