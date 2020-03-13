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

case class UacResources (
  service: Option[ServiceEnumService] = None,
  resource_ids: Option[List[String]] = None,
  role_service_resource_type: Option[RoleResourceEnumRoleServiceResourceTypes] = None,
  authz_service_resource_type: Option[AuthzResourceEnumAuthzServiceResourceTypes] = None,
  modeldb_service_resource_type: Option[ModelResourceEnumModelDBServiceResourceTypes] = None
) extends BaseSwagger {
  def toJson(): JValue = UacResources.toJson(this)
}

object UacResources {
  def toJson(obj: UacResources): JObject = {
    new JObject(
      List[Option[JField]](
        obj.service.map(x => JField("service", ((x: ServiceEnumService) => ServiceEnumService.toJson(x))(x))),
        obj.resource_ids.map(x => JField("resource_ids", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.role_service_resource_type.map(x => JField("role_service_resource_type", ((x: RoleResourceEnumRoleServiceResourceTypes) => RoleResourceEnumRoleServiceResourceTypes.toJson(x))(x))),
        obj.authz_service_resource_type.map(x => JField("authz_service_resource_type", ((x: AuthzResourceEnumAuthzServiceResourceTypes) => AuthzResourceEnumAuthzServiceResourceTypes.toJson(x))(x))),
        obj.modeldb_service_resource_type.map(x => JField("modeldb_service_resource_type", ((x: ModelResourceEnumModelDBServiceResourceTypes) => ModelResourceEnumModelDBServiceResourceTypes.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacResources =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacResources(
          // TODO: handle required
          service = fieldsMap.get("service").map(ServiceEnumService.fromJson),
          resource_ids = fieldsMap.get("resource_ids").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          role_service_resource_type = fieldsMap.get("role_service_resource_type").map(RoleResourceEnumRoleServiceResourceTypes.fromJson),
          authz_service_resource_type = fieldsMap.get("authz_service_resource_type").map(AuthzResourceEnumAuthzServiceResourceTypes.fromJson),
          modeldb_service_resource_type = fieldsMap.get("modeldb_service_resource_type").map(ModelResourceEnumModelDBServiceResourceTypes.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
