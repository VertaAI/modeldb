package ai.verta.repository

/** A class to represent the output of Commit's walk method.
 *  User should not instantiate this.
 *  @param folderPath path to current folder
 *  @param folderNames names of subfolders in current folder
 *  @param blobNames names of blobs in current folder
 *  @param remainingLocations list of remaining locations
 */
final case class WalkOutput(
  val folderPath: String,
  val folderNames: Option[List[String]],
  val blobNames: Option[List[String]],
  val remainingLocations: List[PathList]
) {
  /** Full path to the blobs.
   *  User can use elements of this list to get blobs from commit.
   */
  def blobPaths = blobNames.map(_.map(fullPath))

  /** Full path to the subfolders.
   */
  def subfolderPaths = folderNames.map(_.map(fullPath))

  /** Extend a relative path inside the folder into full path
   *  Use to calculate full paths of blobs and subfolders
   */
  private def fullPath(path: String) = {
    if (folderPath.length > 0) f"${folderPath}/${path}"
    else path
  }
}
