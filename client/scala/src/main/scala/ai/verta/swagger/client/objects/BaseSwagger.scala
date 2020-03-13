package ai.verta.swagger.client.objects

import net.liftweb.json.JValue

trait BaseSwagger {
  def toJson(): JValue
}
