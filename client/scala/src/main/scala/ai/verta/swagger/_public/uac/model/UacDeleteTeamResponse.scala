// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class UacDeleteTeamResponse (
  status: Option[Boolean] = None
) extends BaseSwagger {
  def toJson(): JValue = UacDeleteTeamResponse.toJson(this)
}

object UacDeleteTeamResponse {
  def toJson(obj: UacDeleteTeamResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.status.map(x => JField("status", JBool(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacDeleteTeamResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacDeleteTeamResponse(
          // TODO: handle required
          status = fieldsMap.get("status").map(JsonConverter.fromJsonBoolean)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
