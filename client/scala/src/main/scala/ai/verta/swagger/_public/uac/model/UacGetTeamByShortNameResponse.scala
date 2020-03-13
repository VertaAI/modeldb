// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class UacGetTeamByShortNameResponse (
  team: Option[UacTeam] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetTeamByShortNameResponse.toJson(this)
}

object UacGetTeamByShortNameResponse {
  def toJson(obj: UacGetTeamByShortNameResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.team.map(x => JField("team", ((x: UacTeam) => UacTeam.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacGetTeamByShortNameResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacGetTeamByShortNameResponse(
          // TODO: handle required
          team = fieldsMap.get("team").map(UacTeam.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
