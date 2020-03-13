// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class UacListTeamUserResponse (
  user_ids: Option[List[String]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacListTeamUserResponse.toJson(this)
}

object UacListTeamUserResponse {
  def toJson(obj: UacListTeamUserResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.user_ids.map(x => JField("user_ids", ((x: List[String]) => JArray(x.map(JString)))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacListTeamUserResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacListTeamUserResponse(
          // TODO: handle required
          user_ids = fieldsMap.get("user_ids").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
