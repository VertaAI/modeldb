// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class UacWorkspace (
  id: Option[BigInt] = None,
  org_id: Option[String] = None,
  org_name: Option[String] = None,
  user_id: Option[String] = None,
  username: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = UacWorkspace.toJson(this)
}

object UacWorkspace {
  def toJson(obj: UacWorkspace): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JInt(x))),
        obj.org_id.map(x => JField("org_id", JString(x))),
        obj.org_name.map(x => JField("org_name", JString(x))),
        obj.user_id.map(x => JField("user_id", JString(x))),
        obj.username.map(x => JField("username", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacWorkspace =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacWorkspace(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonInteger),
          org_id = fieldsMap.get("org_id").map(JsonConverter.fromJsonString),
          org_name = fieldsMap.get("org_name").map(JsonConverter.fromJsonString),
          user_id = fieldsMap.get("user_id").map(JsonConverter.fromJsonString),
          username = fieldsMap.get("username").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
