package ai.verta.repository

import ai.verta.blobs.Blob

/** The walker runs on every file in that given folder and all of its subfolders.
 *  @tparam T Type of the returned value over the walk.
 */
trait FolderWalker[+T] {
  /** Filters files and subfolders to be walked on.
   *  The returned folder can contain less elements than before.
   */
  def filterFolder(folder: Folder): Folder = folder

  /** Replaces the current object based on the information of a given folder.
   *  This can be used to replace the base object with subfolder-specific information.
   */
  def replace(folder: Folder): FolderWalker[T] = this

  /** Visits the given blob.
   */
  def visitBlob(name: String, blob: Blob): T

  /** Visits the given folder.
   */
  def visitFolder(name: String, folder: Folder): T
}
