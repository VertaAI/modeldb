// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.uac.model.ModelDBResourceEnumModelDBServiceResourceTypes._
import ai.verta.swagger._public.uac.model.ServiceEnumService._
import ai.verta.swagger.client.objects._

case class UacGetAllowedEntitiesWithActionsResponse (
  entitiesWithActions: Option[List[UacGetAllowedEntitiesWithActionsResponse]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetAllowedEntitiesWithActionsResponse.toJson(this)
}

object UacGetAllowedEntitiesWithActionsResponse {
  def toJson(obj: UacGetAllowedEntitiesWithActionsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.entitiesWithActions.map(x => JField("entitiesWithActions", ((x: List[UacGetAllowedEntitiesWithActionsResponse]) => JArray(x.map(((x: UacGetAllowedEntitiesWithActionsResponse) => UacGetAllowedEntitiesWithActionsResponse.toJson(x)))))(x)))
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
          entitiesWithActions = fieldsMap.get("entitiesWithActions").map((x: JValue) => x match {case JArray(elements) => elements.map(UacGetAllowedEntitiesWithActionsResponse.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
