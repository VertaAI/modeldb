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

case class UacRoleBinding (
  id: Option[String] = None,
  name: Option[String] = None,
  scope: Option[UacRoleScope] = None,
  entities: Option[List[UacEntities]] = None,
  resources: Option[List[UacResources]] = None,
  role_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = UacRoleBinding.toJson(this)
}

object UacRoleBinding {
  def toJson(obj: UacRoleBinding): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.scope.map(x => JField("scope", ((x: UacRoleScope) => UacRoleScope.toJson(x))(x))),
        obj.entities.map(x => JField("entities", ((x: List[UacEntities]) => JArray(x.map(((x: UacEntities) => UacEntities.toJson(x)))))(x))),
        obj.resources.map(x => JField("resources", ((x: List[UacResources]) => JArray(x.map(((x: UacResources) => UacResources.toJson(x)))))(x))),
        obj.role_id.map(x => JField("role_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacRoleBinding =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacRoleBinding(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          scope = fieldsMap.get("scope").map(UacRoleScope.fromJson),
          entities = fieldsMap.get("entities").map((x: JValue) => x match {case JArray(elements) => elements.map(UacEntities.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          resources = fieldsMap.get("resources").map((x: JValue) => x match {case JArray(elements) => elements.map(UacResources.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          role_id = fieldsMap.get("role_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
