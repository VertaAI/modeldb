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

case class UacListRoleBindingsResponse (
  role_bindings: Option[List[UacRoleBinding]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacListRoleBindingsResponse.toJson(this)
}

object UacListRoleBindingsResponse {
  def toJson(obj: UacListRoleBindingsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.role_bindings.map(x => JField("role_bindings", ((x: List[UacRoleBinding]) => JArray(x.map(((x: UacRoleBinding) => UacRoleBinding.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacListRoleBindingsResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacListRoleBindingsResponse(
          // TODO: handle required
          role_bindings = fieldsMap.get("role_bindings").map((x: JValue) => x match {case JArray(elements) => elements.map(UacRoleBinding.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
