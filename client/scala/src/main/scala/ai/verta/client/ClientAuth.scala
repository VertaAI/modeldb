package ai.verta.client

case class ClientAuth(email: String, devKey: String)

object ClientAuth {
  def fromEnvironment(): ClientAuth = new ClientAuth(
    sys.env.get("VERTA_EMAIL").getOrElse(""),
    sys.env.get("VERTA_DEV_KEY").getOrElse("")
  )
}
