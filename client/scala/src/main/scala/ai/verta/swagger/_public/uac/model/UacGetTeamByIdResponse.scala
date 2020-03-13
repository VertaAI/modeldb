// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class UacGetTeamByIdResponse (
  team: Option[UacTeam] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetTeamByIdResponse.toJson(this)
}

object UacGetTeamByIdResponse {
  def toJson(obj: UacGetTeamByIdResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.team.map(x => JField("team", ((x: UacTeam) => UacTeam.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacGetTeamByIdResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacGetTeamByIdResponse(
          // TODO: handle required
          team = fieldsMap.get("team").map(UacTeam.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
