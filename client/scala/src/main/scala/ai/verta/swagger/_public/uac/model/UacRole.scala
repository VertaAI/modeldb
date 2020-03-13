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

case class UacRole (
  id: Option[String] = None,
  name: Option[String] = None,
  scope: Option[UacRoleScope] = None,
  resource_action_groups: Option[List[UacResourceActionGroup]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacRole.toJson(this)
}

object UacRole {
  def toJson(obj: UacRole): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.scope.map(x => JField("scope", ((x: UacRoleScope) => UacRoleScope.toJson(x))(x))),
        obj.resource_action_groups.map(x => JField("resource_action_groups", ((x: List[UacResourceActionGroup]) => JArray(x.map(((x: UacResourceActionGroup) => UacResourceActionGroup.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacRole =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacRole(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          scope = fieldsMap.get("scope").map(UacRoleScope.fromJson),
          resource_action_groups = fieldsMap.get("resource_action_groups").map((x: JValue) => x match {case JArray(elements) => elements.map(UacResourceActionGroup.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
