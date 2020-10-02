// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class VersioningAuditLog (
  action: Option[String] = None,
  local_id: Option[String] = None,
  metadata_blob: Option[String] = None,
  resource: Option[VersioningAuditResource] = None,
  ts_nano: Option[BigInt] = None,
  user: Option[VersioningAuditUser] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningAuditLog.toJson(this)
}

object VersioningAuditLog {
  def toJson(obj: VersioningAuditLog): JObject = {
    new JObject(
      List[Option[JField]](
        obj.action.map(x => JField("action", JString(x))),
        obj.local_id.map(x => JField("local_id", JString(x))),
        obj.metadata_blob.map(x => JField("metadata_blob", JString(x))),
        obj.resource.map(x => JField("resource", ((x: VersioningAuditResource) => VersioningAuditResource.toJson(x))(x))),
        obj.ts_nano.map(x => JField("ts_nano", JInt(x))),
        obj.user.map(x => JField("user", ((x: VersioningAuditUser) => VersioningAuditUser.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningAuditLog =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningAuditLog(
          // TODO: handle required
          action = fieldsMap.get("action").map(JsonConverter.fromJsonString),
          local_id = fieldsMap.get("local_id").map(JsonConverter.fromJsonString),
          metadata_blob = fieldsMap.get("metadata_blob").map(JsonConverter.fromJsonString),
          resource = fieldsMap.get("resource").map(VersioningAuditResource.fromJson),
          ts_nano = fieldsMap.get("ts_nano").map(JsonConverter.fromJsonInteger),
          user = fieldsMap.get("user").map(VersioningAuditUser.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
