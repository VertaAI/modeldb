// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class UacListMyTeamsResponse (
  teams: Option[List[UacTeam]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacListMyTeamsResponse.toJson(this)
}

object UacListMyTeamsResponse {
  def toJson(obj: UacListMyTeamsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.teams.map(x => JField("teams", ((x: List[UacTeam]) => JArray(x.map(((x: UacTeam) => UacTeam.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacListMyTeamsResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacListMyTeamsResponse(
          // TODO: handle required
          teams = fieldsMap.get("teams").map((x: JValue) => x match {case JArray(elements) => elements.map(UacTeam.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
