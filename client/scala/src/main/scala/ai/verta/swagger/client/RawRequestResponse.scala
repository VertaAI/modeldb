package ai.verta.swagger.client

import sttp.model._
import sttp.client._

/** Represents the responses of HttpClient's raw requests
 *  @param body body of the response
 *  @param headers headers of the response
*/
case class RawRequestResponse(body: Array[Byte], headers: String => Option[String]) {}
