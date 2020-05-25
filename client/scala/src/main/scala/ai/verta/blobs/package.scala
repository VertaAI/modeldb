package ai.verta

import ai.verta.swagger._public.modeldb.versioning.model._

package object blobs {
  /** Helper function to convert a VersioningBlob instance to corresponding Blob subclass instance
   *  TODO: finish the pattern matching with other blob subclasses
   */
  def versioningBlobToBlob(vb: VersioningBlob): Blob = vb match {
    case VersioningBlob(
      Some(VersioningCodeBlob(Some(VersioningGitCodeBlob(branch, hash, is_dirty, repo, tag)), _)),
      _, _, _) => Git(branch, hash, is_dirty, repo, tag)
  }
}
