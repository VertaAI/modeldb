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

case class UacAction (
  service: Option[ServiceEnumService] = None,
  role_service_action: Option[RoleActionEnumRoleServiceActions] = None,
  authz_service_action: Option[AuthzActionEnumAuthzServiceActions] = None,
  modeldb_service_action: Option[ModelDBActionEnumModelDBServiceActions] = None
) extends BaseSwagger {
  def toJson(): JValue = UacAction.toJson(this)
}

object UacAction {
  def toJson(obj: UacAction): JObject = {
    new JObject(
      List[Option[JField]](
        obj.service.map(x => JField("service", ((x: ServiceEnumService) => ServiceEnumService.toJson(x))(x))),
        obj.role_service_action.map(x => JField("role_service_action", ((x: RoleActionEnumRoleServiceActions) => RoleActionEnumRoleServiceActions.toJson(x))(x))),
        obj.authz_service_action.map(x => JField("authz_service_action", ((x: AuthzActionEnumAuthzServiceActions) => AuthzActionEnumAuthzServiceActions.toJson(x))(x))),
        obj.modeldb_service_action.map(x => JField("modeldb_service_action", ((x: ModelDBActionEnumModelDBServiceActions) => ModelDBActionEnumModelDBServiceActions.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacAction =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacAction(
          // TODO: handle required
          service = fieldsMap.get("service").map(ServiceEnumService.fromJson),
          role_service_action = fieldsMap.get("role_service_action").map(RoleActionEnumRoleServiceActions.fromJson),
          authz_service_action = fieldsMap.get("authz_service_action").map(AuthzActionEnumAuthzServiceActions.fromJson),
          modeldb_service_action = fieldsMap.get("modeldb_service_action").map(ModelDBActionEnumModelDBServiceActions.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
