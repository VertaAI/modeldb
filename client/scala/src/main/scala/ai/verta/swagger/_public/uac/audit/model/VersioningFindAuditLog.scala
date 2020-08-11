// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class VersioningFindAuditLog (
  pagination: Option[CommonPagination] = None,
  query: Option[VersioningAuditLogPredicates] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningFindAuditLog.toJson(this)
}

object VersioningFindAuditLog {
  def toJson(obj: VersioningFindAuditLog): JObject = {
    new JObject(
      List[Option[JField]](
        obj.pagination.map(x => JField("pagination", ((x: CommonPagination) => CommonPagination.toJson(x))(x))),
        obj.query.map(x => JField("query", ((x: VersioningAuditLogPredicates) => VersioningAuditLogPredicates.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningFindAuditLog =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningFindAuditLog(
          // TODO: handle required
          pagination = fieldsMap.get("pagination").map(CommonPagination.fromJson),
          query = fieldsMap.get("query").map(VersioningAuditLogPredicates.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
