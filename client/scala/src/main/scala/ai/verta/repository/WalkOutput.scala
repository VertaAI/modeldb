package ai.verta.repository

/** A class to represent the output of Commit's walk method
 *  @param folderPath path to current folder
 *  @param folderNames names of subfolders in current folder
 *  @param blobNames names of blobs in current folder
*/
class WalkOutput(val folderPath: String, val folderNames: Option[List[String]], val blobNames: Option[List[String]]) {}
