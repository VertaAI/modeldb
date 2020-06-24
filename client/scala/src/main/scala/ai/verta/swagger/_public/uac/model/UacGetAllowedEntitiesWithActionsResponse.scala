// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.uac.model.ModelDBResourceEnumModelDBServiceResourceTypes._
import ai.verta.swagger._public.uac.model.ServiceEnumService._
import ai.verta.swagger.client.objects._

case class UacGetAllowedEntitiesWithActionsResponse (
  actions: Option[UacAction] = None,
  entities: Option[List[UacEntities]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetAllowedEntitiesWithActionsResponse.toJson(this)
}

object UacGetAllowedEntitiesWithActionsResponse {
  def toJson(obj: UacGetAllowedEntitiesWithActionsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.actions.map(x => JField("actions", ((x: UacAction) => UacAction.toJson(x))(x))),
        obj.entities.map(x => JField("entities", ((x: List[UacEntities]) => JArray(x.map(((x: UacEntities) => UacEntities.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacGetAllowedEntitiesWithActionsResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacGetAllowedEntitiesWithActionsResponse(
          // TODO: handle required
          actions = fieldsMap.get("actions").map(UacAction.fromJson),
          entities = fieldsMap.get("entities").map((x: JValue) => x match {case JArray(elements) => elements.map(UacEntities.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
