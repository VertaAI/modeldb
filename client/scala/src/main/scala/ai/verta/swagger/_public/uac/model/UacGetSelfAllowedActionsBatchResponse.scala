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

case class UacGetSelfAllowedActionsBatchResponse (
  actions: Option[Map[String,UacActions]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetSelfAllowedActionsBatchResponse.toJson(this)
}

object UacGetSelfAllowedActionsBatchResponse {
  def toJson(obj: UacGetSelfAllowedActionsBatchResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.actions.map(x => JField("actions", ((x: Map[String,UacActions]) => JObject(x.toList.map(kv => JField(kv._1,((x: UacActions) => UacActions.toJson(x))(kv._2)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacGetSelfAllowedActionsBatchResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacGetSelfAllowedActionsBatchResponse(
          // TODO: handle required
          actions = fieldsMap.get("actions").map((x: JValue) => x match {case JObject(fields) => fields.map(kv => (kv.name, UacActions.fromJson(kv.value))).toMap; case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
