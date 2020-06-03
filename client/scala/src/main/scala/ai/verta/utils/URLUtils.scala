package ai.verta.utils

import java.net.URLEncoder

object URLUtils {
  def urlEncode(input: String): String = URLEncoder.encode(input, "UTF-8").replaceAll("\\+", "%20")
}
