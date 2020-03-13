// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.uac.model.TernaryEnumTernary._
import ai.verta.swagger.client.objects._

case class UacListMyOrganizationsResponse (
  organizations: Option[List[UacOrganization]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacListMyOrganizationsResponse.toJson(this)
}

object UacListMyOrganizationsResponse {
  def toJson(obj: UacListMyOrganizationsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.organizations.map(x => JField("organizations", ((x: List[UacOrganization]) => JArray(x.map(((x: UacOrganization) => UacOrganization.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacListMyOrganizationsResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacListMyOrganizationsResponse(
          // TODO: handle required
          organizations = fieldsMap.get("organizations").map((x: JValue) => x match {case JArray(elements) => elements.map(UacOrganization.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
