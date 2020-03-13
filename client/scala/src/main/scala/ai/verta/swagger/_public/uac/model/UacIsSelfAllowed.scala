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

case class UacIsSelfAllowed (
  actions: Option[List[UacAction]] = None,
  resources: Option[List[UacResources]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacIsSelfAllowed.toJson(this)
}

object UacIsSelfAllowed {
  def toJson(obj: UacIsSelfAllowed): JObject = {
    new JObject(
      List[Option[JField]](
        obj.actions.map(x => JField("actions", ((x: List[UacAction]) => JArray(x.map(((x: UacAction) => UacAction.toJson(x)))))(x))),
        obj.resources.map(x => JField("resources", ((x: List[UacResources]) => JArray(x.map(((x: UacResources) => UacResources.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacIsSelfAllowed =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacIsSelfAllowed(
          // TODO: handle required
          actions = fieldsMap.get("actions").map((x: JValue) => x match {case JArray(elements) => elements.map(UacAction.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          resources = fieldsMap.get("resources").map((x: JValue) => x match {case JArray(elements) => elements.map(UacResources.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
