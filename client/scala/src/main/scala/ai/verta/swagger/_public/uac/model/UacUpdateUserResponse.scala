// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.uac.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class UacUpdateUserResponse (
  info: Option[UacUserInfo] = None
) extends BaseSwagger {
  def toJson(): JValue = UacUpdateUserResponse.toJson(this)
}

object UacUpdateUserResponse {
  def toJson(obj: UacUpdateUserResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.info.map(x => JField("info", ((x: UacUserInfo) => UacUserInfo.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacUpdateUserResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacUpdateUserResponse(
          // TODO: handle required
          info = fieldsMap.get("info").map(UacUserInfo.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
