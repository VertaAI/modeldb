// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.uac.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class UacGetUsersResponse (
  user_infos: Option[List[UacUserInfo]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetUsersResponse.toJson(this)
}

object UacGetUsersResponse {
  def toJson(obj: UacGetUsersResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.user_infos.map(x => JField("user_infos", ((x: List[UacUserInfo]) => JArray(x.map(((x: UacUserInfo) => UacUserInfo.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacGetUsersResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacGetUsersResponse(
          // TODO: handle required
          user_infos = fieldsMap.get("user_infos").map((x: JValue) => x match {case JArray(elements) => elements.map(UacUserInfo.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
