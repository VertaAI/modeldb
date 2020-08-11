// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class VersioningUserPredicate (
  user: Option[VersioningAuditUser] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningUserPredicate.toJson(this)
}

object VersioningUserPredicate {
  def toJson(obj: VersioningUserPredicate): JObject = {
    new JObject(
      List[Option[JField]](
        obj.user.map(x => JField("user", ((x: VersioningAuditUser) => VersioningAuditUser.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningUserPredicate =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningUserPredicate(
          // TODO: handle required
          user = fieldsMap.get("user").map(VersioningAuditUser.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
