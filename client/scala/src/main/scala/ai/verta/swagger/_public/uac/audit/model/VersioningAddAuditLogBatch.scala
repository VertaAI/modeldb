// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class VersioningAddAuditLogBatch (
  log: Option[List[VersioningAuditLog]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningAddAuditLogBatch.toJson(this)
}

object VersioningAddAuditLogBatch {
  def toJson(obj: VersioningAddAuditLogBatch): JObject = {
    new JObject(
      List[Option[JField]](
        obj.log.map(x => JField("log", ((x: List[VersioningAuditLog]) => JArray(x.map(((x: VersioningAuditLog) => VersioningAuditLog.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningAddAuditLogBatch =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningAddAuditLogBatch(
          // TODO: handle required
          log = fieldsMap.get("log").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningAuditLog.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
