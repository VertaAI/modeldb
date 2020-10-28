package ai.verta.client

import java.net.URI

case class ClientConnection(host: String, ignoreConnErr: Boolean = false, maxRetries: Int = 5, auth: ClientAuth = null)

object ClientConnection {
  private val DefaultScheme = "https"

  private def hasScheme(url: String) = (new URI(url)).getScheme() != null

  private def urlWithScheme(url: String) =
    if (hasScheme(url)) url else (new URI(DefaultScheme, url, null, null, null)).toString()

  def fromEnvironment(): ClientConnection = new ClientConnection(urlWithScheme(sys.env("VERTA_HOST")), auth = ClientAuth.fromEnvironment())
}
