package ai.verta.blobs.dataset


/** Contain helper method for testing metadata of Dataset blobs */
object TestMetadata {
  /** Verify that a FileMetadata has correct path and does not have invalid parameter
   *  @param metadata file's metadata
   *  @param path file's path (to be checked)
   */
  def assertMetadata(metadata: FileMetadata, path: String) = {
    assert(metadata.path.equals(path))
    assert(metadata.size > 0)
    assert(metadata.lastModified > 0)
    assert(metadata.md5.length > 0)
  }
}
