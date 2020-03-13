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

case class UacIsSelfAllowedResponse (
  allowed: Option[Boolean] = None
) extends BaseSwagger {
  def toJson(): JValue = UacIsSelfAllowedResponse.toJson(this)
}

object UacIsSelfAllowedResponse {
  def toJson(obj: UacIsSelfAllowedResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.allowed.map(x => JField("allowed", JBool(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacIsSelfAllowedResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacIsSelfAllowedResponse(
          // TODO: handle required
          allowed = fieldsMap.get("allowed").map(JsonConverter.fromJsonBoolean)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
