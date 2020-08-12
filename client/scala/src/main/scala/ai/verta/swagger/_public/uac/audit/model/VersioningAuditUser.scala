// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class VersioningAuditUser (
  user_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningAuditUser.toJson(this)
}

object VersioningAuditUser {
  def toJson(obj: VersioningAuditUser): JObject = {
    new JObject(
      List[Option[JField]](
        obj.user_id.map(x => JField("user_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningAuditUser =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningAuditUser(
          // TODO: handle required
          user_id = fieldsMap.get("user_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
