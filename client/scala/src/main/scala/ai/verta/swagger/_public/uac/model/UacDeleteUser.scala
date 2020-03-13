// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.uac.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class UacDeleteUser (
  user_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = UacDeleteUser.toJson(this)
}

object UacDeleteUser {
  def toJson(obj: UacDeleteUser): JObject = {
    new JObject(
      List[Option[JField]](
        obj.user_id.map(x => JField("user_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacDeleteUser =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacDeleteUser(
          // TODO: handle required
          user_id = fieldsMap.get("user_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
