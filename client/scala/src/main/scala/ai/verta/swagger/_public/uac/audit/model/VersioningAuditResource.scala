// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class VersioningAuditResource (
  resource_id: Option[String] = None,
  resource_service: Option[String] = None,
  resource_type: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningAuditResource.toJson(this)
}

object VersioningAuditResource {
  def toJson(obj: VersioningAuditResource): JObject = {
    new JObject(
      List[Option[JField]](
        obj.resource_id.map(x => JField("resource_id", JString(x))),
        obj.resource_service.map(x => JField("resource_service", JString(x))),
        obj.resource_type.map(x => JField("resource_type", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningAuditResource =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningAuditResource(
          // TODO: handle required
          resource_id = fieldsMap.get("resource_id").map(JsonConverter.fromJsonString),
          resource_service = fieldsMap.get("resource_service").map(JsonConverter.fromJsonString),
          resource_type = fieldsMap.get("resource_type").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
