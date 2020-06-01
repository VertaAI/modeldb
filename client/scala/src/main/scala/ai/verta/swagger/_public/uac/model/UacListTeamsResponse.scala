// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.uac.model.TernaryEnumTernary._
import ai.verta.swagger.client.objects._

case class UacListTeamsResponse (
  team_ids: Option[List[String]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacListTeamsResponse.toJson(this)
}

object UacListTeamsResponse {
  def toJson(obj: UacListTeamsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.team_ids.map(x => JField("team_ids", ((x: List[String]) => JArray(x.map(JString)))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacListTeamsResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacListTeamsResponse(
          // TODO: handle required
          team_ids = fieldsMap.get("team_ids").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
