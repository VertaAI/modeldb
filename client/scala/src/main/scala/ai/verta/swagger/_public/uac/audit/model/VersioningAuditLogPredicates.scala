// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class VersioningAuditLogPredicates (
  resource_predicate: Option[VersioningResourcePredicate] = None,
  timestamp_predicate: Option[VersioningRangeTimeStampPredicate] = None,
  user_predicate: Option[VersioningUserPredicate] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningAuditLogPredicates.toJson(this)
}

object VersioningAuditLogPredicates {
  def toJson(obj: VersioningAuditLogPredicates): JObject = {
    new JObject(
      List[Option[JField]](
        obj.resource_predicate.map(x => JField("resource_predicate", ((x: VersioningResourcePredicate) => VersioningResourcePredicate.toJson(x))(x))),
        obj.timestamp_predicate.map(x => JField("timestamp_predicate", ((x: VersioningRangeTimeStampPredicate) => VersioningRangeTimeStampPredicate.toJson(x))(x))),
        obj.user_predicate.map(x => JField("user_predicate", ((x: VersioningUserPredicate) => VersioningUserPredicate.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningAuditLogPredicates =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningAuditLogPredicates(
          // TODO: handle required
          resource_predicate = fieldsMap.get("resource_predicate").map(VersioningResourcePredicate.fromJson),
          timestamp_predicate = fieldsMap.get("timestamp_predicate").map(VersioningRangeTimeStampPredicate.fromJson),
          user_predicate = fieldsMap.get("user_predicate").map(VersioningUserPredicate.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
