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

case class UacGetAllowedResources (
  entities: Option[List[UacEntities]] = None,
  actions: Option[List[UacAction]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetAllowedResources.toJson(this)
}

object UacGetAllowedResources {
  def toJson(obj: UacGetAllowedResources): JObject = {
    new JObject(
      List[Option[JField]](
        obj.entities.map(x => JField("entities", ((x: List[UacEntities]) => JArray(x.map(((x: UacEntities) => UacEntities.toJson(x)))))(x))),
        obj.actions.map(x => JField("actions", ((x: List[UacAction]) => JArray(x.map(((x: UacAction) => UacAction.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacGetAllowedResources =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacGetAllowedResources(
          // TODO: handle required
          entities = fieldsMap.get("entities").map((x: JValue) => x match {case JArray(elements) => elements.map(UacEntities.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          actions = fieldsMap.get("actions").map((x: JValue) => x match {case JArray(elements) => elements.map(UacAction.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
