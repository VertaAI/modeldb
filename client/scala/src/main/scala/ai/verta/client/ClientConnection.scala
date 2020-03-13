package ai.verta.client

case class ClientConnection(host: String, ignoreConnErr: Boolean = false, maxRetries: Int = 5, auth: ClientAuth = null)

object ClientConnection {
  def fromEnvironment(): ClientConnection = new ClientConnection(sys.env("VERTA_HOST"), auth = ClientAuth.fromEnvironment())
}
