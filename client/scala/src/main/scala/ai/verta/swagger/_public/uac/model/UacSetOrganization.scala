// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.uac.model.TernaryEnumTernary._
import ai.verta.swagger.client.objects._

case class UacSetOrganization (
  organization: Option[UacOrganization] = None
) extends BaseSwagger {
  def toJson(): JValue = UacSetOrganization.toJson(this)
}

object UacSetOrganization {
  def toJson(obj: UacSetOrganization): JObject = {
    new JObject(
      List[Option[JField]](
        obj.organization.map(x => JField("organization", ((x: UacOrganization) => UacOrganization.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacSetOrganization =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacSetOrganization(
          // TODO: handle required
          organization = fieldsMap.get("organization").map(UacOrganization.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
