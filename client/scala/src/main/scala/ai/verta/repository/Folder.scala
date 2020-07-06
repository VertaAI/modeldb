package ai.verta.repository

/** Represent a folder visited during Commit's walk.
 *  @param root root path of the folder
 *  @param subfolders names of the subfolders of the folder
 *  @param blobs names of the blobs stored in the folder
 */
case class Folder(root: String, blobs: List[String], subfolders: List[String]) {
  /** Filter the blobs based on a predicate
   *  @param fn predicate
   *  @return a new Folder instance, with the blobs filtered by fn
   */
  def filterBlobs(fn: String => Boolean) = Folder(root, blobs.filter(fn), subfolders)

  /** Filter the subfolders based on a predicate
   *  @param fn predicate
   *  @return a new Folder instance, with the subfolders filtered by fn
   */
  def filterSubfolders(fn: String => Boolean) = Folder(root, blobs, subfolders.filter(fn))

  /** Full path to the blobs.
   *  User can use elements of this list to get blobs from commit.
   */
  private[repository] def blobPaths = blobs.map(fullPath)

  /** Full path to the subfolders.
   */
  private[repository] def subfolderPaths = subfolders.map(fullPath)

  /** Extend a relative path inside the folder into full path
   *  Use to calculate full paths of blobs and subfolders
   */
  private def fullPath(path: String) = {
    if (root.length > 0) f"${root}/${path}"
    else path
  }
}
