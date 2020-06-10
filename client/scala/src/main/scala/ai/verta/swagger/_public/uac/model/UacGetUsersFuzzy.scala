// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.uac.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class UacGetUsersFuzzy (
  email: Option[String] = None,
  pagination: Option[VertauacPagination] = None,
  username: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetUsersFuzzy.toJson(this)
}

object UacGetUsersFuzzy {
  def toJson(obj: UacGetUsersFuzzy): JObject = {
    new JObject(
      List[Option[JField]](
        obj.email.map(x => JField("email", JString(x))),
        obj.pagination.map(x => JField("pagination", ((x: VertauacPagination) => VertauacPagination.toJson(x))(x))),
        obj.username.map(x => JField("username", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacGetUsersFuzzy =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacGetUsersFuzzy(
          // TODO: handle required
          email = fieldsMap.get("email").map(JsonConverter.fromJsonString),
          pagination = fieldsMap.get("pagination").map(VertauacPagination.fromJson),
          username = fieldsMap.get("username").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
