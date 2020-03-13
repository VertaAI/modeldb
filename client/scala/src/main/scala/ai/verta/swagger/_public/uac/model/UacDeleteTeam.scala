// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class UacDeleteTeam (
  team_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = UacDeleteTeam.toJson(this)
}

object UacDeleteTeam {
  def toJson(obj: UacDeleteTeam): JObject = {
    new JObject(
      List[Option[JField]](
        obj.team_id.map(x => JField("team_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacDeleteTeam =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacDeleteTeam(
          // TODO: handle required
          team_id = fieldsMap.get("team_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
