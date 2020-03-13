// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.uac.model.TernaryEnumTernary._
import ai.verta.swagger.client.objects._

case class UacRemoveUser (
  org_id: Option[String] = None,
  share_with: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = UacRemoveUser.toJson(this)
}

object UacRemoveUser {
  def toJson(obj: UacRemoveUser): JObject = {
    new JObject(
      List[Option[JField]](
        obj.org_id.map(x => JField("org_id", JString(x))),
        obj.share_with.map(x => JField("share_with", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacRemoveUser =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacRemoveUser(
          // TODO: handle required
          org_id = fieldsMap.get("org_id").map(JsonConverter.fromJsonString),
          share_with = fieldsMap.get("share_with").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
