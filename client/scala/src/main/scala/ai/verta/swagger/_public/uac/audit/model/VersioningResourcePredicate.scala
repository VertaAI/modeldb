// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class VersioningResourcePredicate (
  resource: Option[VersioningAuditResource] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningResourcePredicate.toJson(this)
}

object VersioningResourcePredicate {
  def toJson(obj: VersioningResourcePredicate): JObject = {
    new JObject(
      List[Option[JField]](
        obj.resource.map(x => JField("resource", ((x: VersioningAuditResource) => VersioningAuditResource.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningResourcePredicate =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningResourcePredicate(
          // TODO: handle required
          resource = fieldsMap.get("resource").map(VersioningAuditResource.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
