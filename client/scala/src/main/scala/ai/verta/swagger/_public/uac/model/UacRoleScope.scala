// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.AuthzActionEnumAuthzServiceActions._
import ai.verta.swagger._public.uac.model.AuthzResourceEnumAuthzServiceResourceTypes._
import ai.verta.swagger._public.uac.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.uac.model.ModelResourceEnumModelDBServiceResourceTypes._
import ai.verta.swagger._public.uac.model.RoleActionEnumRoleServiceActions._
import ai.verta.swagger._public.uac.model.RoleResourceEnumRoleServiceResourceTypes._
import ai.verta.swagger._public.uac.model.ServiceEnumService._
import ai.verta.swagger.client.objects._

case class UacRoleScope (
  org_id: Option[String] = None,
  team_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = UacRoleScope.toJson(this)
}

object UacRoleScope {
  def toJson(obj: UacRoleScope): JObject = {
    new JObject(
      List[Option[JField]](
        obj.org_id.map(x => JField("org_id", JString(x))),
        obj.team_id.map(x => JField("team_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacRoleScope =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacRoleScope(
          // TODO: handle required
          org_id = fieldsMap.get("org_id").map(JsonConverter.fromJsonString),
          team_id = fieldsMap.get("team_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
