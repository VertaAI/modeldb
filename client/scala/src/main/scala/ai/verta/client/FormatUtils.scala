package ai.verta.client

import java.net.URLEncoder

/** Containing the helper functions to format the input string */
object FormatUtils {
  /** Utility method for HTML form encoding
   *  @param input input
   */
  def urlEncode(input: String): String = URLEncoder.encode(input, "UTF-8").replaceAll("\\+", "%20")
}
