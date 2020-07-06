// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.uac.model.ModelDBResourceEnumModelDBServiceResourceTypes._
import ai.verta.swagger._public.uac.model.ServiceEnumService._
import ai.verta.swagger.client.objects._

case class UacGetSelfAllowedResources (
  actions: Option[List[UacAction]] = None,
  resource_type: Option[UacResourceType] = None,
  service: Option[ServiceEnumService] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetSelfAllowedResources.toJson(this)
}

object UacGetSelfAllowedResources {
  def toJson(obj: UacGetSelfAllowedResources): JObject = {
    new JObject(
      List[Option[JField]](
        obj.actions.map(x => JField("actions", ((x: List[UacAction]) => JArray(x.map(((x: UacAction) => UacAction.toJson(x)))))(x))),
        obj.resource_type.map(x => JField("resource_type", ((x: UacResourceType) => UacResourceType.toJson(x))(x))),
        obj.service.map(x => JField("service", ((x: ServiceEnumService) => ServiceEnumService.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacGetSelfAllowedResources =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacGetSelfAllowedResources(
          // TODO: handle required
          actions = fieldsMap.get("actions").map((x: JValue) => x match {case JArray(elements) => elements.map(UacAction.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          resource_type = fieldsMap.get("resource_type").map(UacResourceType.fromJson),
          service = fieldsMap.get("service").map(ServiceEnumService.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
