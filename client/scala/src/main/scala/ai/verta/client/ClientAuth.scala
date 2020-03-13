package ai.verta.client

case class ClientAuth(email: String, devKey: String)

object ClientAuth {
  def fromEnvironment(): ClientAuth = new ClientAuth(sys.env("VERTA_EMAIL"), sys.env("VERTA_DEV_KEY"))
}
