package ai.verta

import scala.concurrent.ExecutionContext
import java.net.URLEncoder

package object client {
  // TODO: implement getting personal workspace functionality.
  /** Get the user's personal workspace
   */
  def getPersonalWorkspace()(implicit ec: ExecutionContext): String = {
    "personal"
  }

  def urlEncode(input: String): String = URLEncoder.encode(input, "UTF-8").replaceAll("\\+", "%20")
}
