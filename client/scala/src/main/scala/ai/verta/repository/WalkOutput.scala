package ai.verta.repository

/** A class to represent the output of Commit's walk method.
 *  User should not instantiate this.
 *  @param folderPath path to current folder
 *  @param folderNames names of subfolders in current folder
 *  @param blobNames names of blobs in current folder
 *  @param remainingLocations list of remaining locations
 */
class WalkOutput(
  val folderPath: String,
  val folderNames: Option[List[String]],
  val blobNames: Option[List[String]],
  val remainingLocations: List[List[String]]
) {}
