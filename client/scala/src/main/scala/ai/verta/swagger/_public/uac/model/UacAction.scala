// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.uac.model.ModelDBResourceEnumModelDBServiceResourceTypes._
import ai.verta.swagger._public.uac.model.ServiceEnumService._
import ai.verta.swagger.client.objects._

case class UacAction (
  modeldb_service_action: Option[ModelDBActionEnumModelDBServiceActions] = None,
  service: Option[ServiceEnumService] = None
) extends BaseSwagger {
  def toJson(): JValue = UacAction.toJson(this)
}

object UacAction {
  def toJson(obj: UacAction): JObject = {
    new JObject(
      List[Option[JField]](
        obj.modeldb_service_action.map(x => JField("modeldb_service_action", ((x: ModelDBActionEnumModelDBServiceActions) => ModelDBActionEnumModelDBServiceActions.toJson(x))(x))),
        obj.service.map(x => JField("service", ((x: ServiceEnumService) => ServiceEnumService.toJson(x))(x)))
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
          modeldb_service_action = fieldsMap.get("modeldb_service_action").map(ModelDBActionEnumModelDBServiceActions.fromJson),
          service = fieldsMap.get("service").map(ServiceEnumService.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
