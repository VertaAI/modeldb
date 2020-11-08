package ai.verta.client

import java.net.URI

case class ClientConnection(host: String, ignoreConnErr: Boolean = false, maxRetries: Int = 5, auth: ClientAuth = null)

object ClientConnection {
  private val AllowedScheme = List("http", "https")
  private val DefaultScheme = "https"
  private val DefaultSchemeOSS = "http" // https://localhost:3000 does not work

  private def urlWithScheme(url: String) = {
    val scheme = (new URI(url)).getScheme()

    if (scheme == "localhost")
      f"${DefaultSchemeOSS}://${url}"
    else if (scheme != null && AllowedScheme.contains(scheme))
        url
    else if (scheme == null)
      f"${DefaultScheme}://${url}"
    else
      throw new IllegalArgumentException("VERTA_HOST has invalid scheme")
  }

  def fromEnvironment(): ClientConnection = new ClientConnection(urlWithScheme(sys.env("VERTA_HOST")), auth = ClientAuth.fromEnvironment())
}
