package ai.verta.blobs.dataset

/** Output of Dataset's determineComponentAndLocalPaths method
 *  @param componentToLocalPath a map from component path to local path to download to
 *  @param absoluteLocalPath absolute local path to the downloaded file(s)
 */
private[dataset] case class ComponentToLocalPath(
  val componentToLocalPathMap: Map[String, String], val absoluteLocalPath: String
)
