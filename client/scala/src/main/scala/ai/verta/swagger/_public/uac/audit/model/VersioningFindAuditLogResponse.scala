// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class VersioningFindAuditLogResponse (
  logs: Option[List[VersioningAuditLog]] = None,
  total_records: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningFindAuditLogResponse.toJson(this)
}

object VersioningFindAuditLogResponse {
  def toJson(obj: VersioningFindAuditLogResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.logs.map(x => JField("logs", ((x: List[VersioningAuditLog]) => JArray(x.map(((x: VersioningAuditLog) => VersioningAuditLog.toJson(x)))))(x))),
        obj.total_records.map(x => JField("total_records", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningFindAuditLogResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningFindAuditLogResponse(
          // TODO: handle required
          logs = fieldsMap.get("logs").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningAuditLog.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          total_records = fieldsMap.get("total_records").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
