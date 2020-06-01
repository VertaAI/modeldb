// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.uac.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class UacCreateUser (
  info: Option[UacUserInfo] = None,
  password: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = UacCreateUser.toJson(this)
}

object UacCreateUser {
  def toJson(obj: UacCreateUser): JObject = {
    new JObject(
      List[Option[JField]](
        obj.info.map(x => JField("info", ((x: UacUserInfo) => UacUserInfo.toJson(x))(x))),
        obj.password.map(x => JField("password", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacCreateUser =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacCreateUser(
          // TODO: handle required
          info = fieldsMap.get("info").map(UacUserInfo.fromJson),
          password = fieldsMap.get("password").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
