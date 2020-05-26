package ai.verta

import ai.verta.swagger._public.modeldb.versioning.model._

package object blobs {
  /** Helper function to convert a VersioningBlob instance to corresponding Blob subclass instance
   *  TODO: finish the pattern matching with other blob subclasses
   */
  def versioningBlobToBlob(vb: VersioningBlob): Blob = vb match {
    case VersioningBlob(Some(VersioningCodeBlob(Some(git), _)), _, _, _) => Git()

    case VersioningBlob(_, _, Some(VersioningDatasetBlob(Some(path), _)), _) => PathBlob(List())
  }


  /** Analogous to os.path.expanduser
   *  From https://stackoverflow.com/questions/6803913/java-analogous-to-python-os-path-expanduser-os-path-expandvars
   *  @param path path
   *  @return path, but with (first occurence of) ~ replace with user's home directory
   */
  def expanduser(path: String) = path.replaceFirst("~", System.getProperty("user.home"))

}
