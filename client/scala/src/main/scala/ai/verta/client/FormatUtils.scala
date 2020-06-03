package ai.verta.client

import java.net.URLEncoder

import net.liftweb.json._
import net.liftweb.json.Serialization.write

/** Containing the helper functions to format the input string */
object FormatUtils {
  /** Utility method for HTML form encoding
   *  @param input input
   */
  def urlEncode(input: String): String = URLEncoder.encode(input, "UTF-8").replaceAll("\\+", "%20")

  /** Utility method to format the string for JSON parsing
   *  @param input input
   */
  def jsonFormat(input: String) = {
    implicit val formats = DefaultFormats
    write(input)
  }
}
