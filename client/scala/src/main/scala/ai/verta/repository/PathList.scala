package ai.verta.repository

/** A class to represent a path in the list of strings format
 *  @param components a list of components of the path
 */
case class PathList(val components: List[String]) {
  /** Return the path in a single string format, with components separated by '/' */
  def path: String = components.mkString("/")

  /** Extend the PathList with another component at the end
   *  @param component the new component
   *  @return a new instance of PathList representing the new path
   */
  def extend(component: String) = PathList(components :+ component)
}
