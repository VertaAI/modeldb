// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class ProtobufAny (
  type_url: Option[String] = None,
  value: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ProtobufAny.toJson(this)
}

object ProtobufAny {
  def toJson(obj: ProtobufAny): JObject = {
    new JObject(
      List[Option[JField]](
        obj.type_url.map(x => JField("type_url", JString(x))),
        obj.value.map(x => JField("value", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ProtobufAny =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ProtobufAny(
          // TODO: handle required
          type_url = fieldsMap.get("type_url").map(JsonConverter.fromJsonString),
          value = fieldsMap.get("value").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
