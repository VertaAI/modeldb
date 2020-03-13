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

case class UacGetSelfAllowedActionsBatch (
  resources: Option[UacResources] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetSelfAllowedActionsBatch.toJson(this)
}

object UacGetSelfAllowedActionsBatch {
  def toJson(obj: UacGetSelfAllowedActionsBatch): JObject = {
    new JObject(
      List[Option[JField]](
        obj.resources.map(x => JField("resources", ((x: UacResources) => UacResources.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacGetSelfAllowedActionsBatch =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacGetSelfAllowedActionsBatch(
          // TODO: handle required
          resources = fieldsMap.get("resources").map(UacResources.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
