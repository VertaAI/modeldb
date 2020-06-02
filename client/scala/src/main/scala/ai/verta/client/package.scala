package ai.verta

import scala.concurrent.ExecutionContext
import java.net.URLEncoder

package object client {
  // TODO: implement getting personal workspace functionality.
  /** Get the user's personal workspace
   */
  def getPersonalWorkspace()(implicit ec: ExecutionContext): String = {
    "personal"
    // val response = clientSet.UACService.getUser(
    //   email = conn.auth.email,
    //   user_id = "abc",
    //   username = "abc"
    // )
    //
    // val ret = response match {
    //   case Success(r) => r.verta_info.map(_.username) match {
    //     case Some(Some(username)) => username
    //     case _ => "personal"
    //   }
    //   case _ => "personal"
    // }
    //
    // println(ret)
    //
    // return ret
  }

  def urlEncode(input: String): String = URLEncoder.encode(input, "UTF-8").replaceAll("\\+", "%20")
}
