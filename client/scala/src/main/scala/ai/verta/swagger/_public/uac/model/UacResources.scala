// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.uac.model.ModelDBResourceEnumModelDBServiceResourceTypes._
import ai.verta.swagger._public.uac.model.ServiceEnumService._
import ai.verta.swagger.client.objects._

case class UacResources (
  resource_ids: Option[List[String]] = None,
  resource_type: Option[UacResourceType] = None,
  service: Option[ServiceEnumService] = None
) extends BaseSwagger {
  def toJson(): JValue = UacResources.toJson(this)
}

object UacResources {
  def toJson(obj: UacResources): JObject = {
    new JObject(
      List[Option[JField]](
        obj.resource_ids.map(x => JField("resource_ids", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.resource_type.map(x => JField("resource_type", ((x: UacResourceType) => UacResourceType.toJson(x))(x))),
        obj.service.map(x => JField("service", ((x: ServiceEnumService) => ServiceEnumService.toJson(x))(x)))
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
          resource_ids = fieldsMap.get("resource_ids").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          resource_type = fieldsMap.get("resource_type").map(UacResourceType.fromJson),
          service = fieldsMap.get("service").map(ServiceEnumService.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
