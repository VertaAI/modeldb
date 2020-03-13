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

case class UacGetRoleByNameResponse (
  role: Option[UacRole] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetRoleByNameResponse.toJson(this)
}

object UacGetRoleByNameResponse {
  def toJson(obj: UacGetRoleByNameResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.role.map(x => JField("role", ((x: UacRole) => UacRole.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacGetRoleByNameResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacGetRoleByNameResponse(
          // TODO: handle required
          role = fieldsMap.get("role").map(UacRole.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
