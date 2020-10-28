package ai.verta.client

import java.net.URI

case class ClientConnection(host: String, ignoreConnErr: Boolean = false, maxRetries: Int = 5, auth: ClientAuth = null)

object ClientConnection {
  private val DefaultScheme = "https"

  private def urlWithScheme(url: String) = {
    val uri = new URI(url)
    val schemeSpecificPart = uri.getSchemeSpecificPart()

    val uriWithScheme = new URI(
      Option(uri.getScheme()).getOrElse(DefaultScheme),
      if (schemeSpecificPart.startsWith("//")) schemeSpecificPart else "//" + schemeSpecificPart,
      uri.getFragment()
    )
    uriWithScheme.toString()
  }

  def fromEnvironment(): ClientConnection = new ClientConnection(urlWithScheme(sys.env("VERTA_HOST")), auth = ClientAuth.fromEnvironment())
}
