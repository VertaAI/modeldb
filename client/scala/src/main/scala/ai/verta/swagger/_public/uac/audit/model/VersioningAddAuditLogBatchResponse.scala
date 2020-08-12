// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class VersioningAddAuditLogBatchResponse (
  response_rows: Option[List[VersioningBatchResponseRow]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningAddAuditLogBatchResponse.toJson(this)
}

object VersioningAddAuditLogBatchResponse {
  def toJson(obj: VersioningAddAuditLogBatchResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.response_rows.map(x => JField("response_rows", ((x: List[VersioningBatchResponseRow]) => JArray(x.map(((x: VersioningBatchResponseRow) => VersioningBatchResponseRow.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningAddAuditLogBatchResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningAddAuditLogBatchResponse(
          // TODO: handle required
          response_rows = fieldsMap.get("response_rows").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningBatchResponseRow.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
