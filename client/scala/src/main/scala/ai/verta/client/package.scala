package ai.verta

import scala.concurrent.ExecutionContext

// TODO: implement getting personal workspace functionality.
/** Get the user's personal workspace
 */
package object client {
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
}
