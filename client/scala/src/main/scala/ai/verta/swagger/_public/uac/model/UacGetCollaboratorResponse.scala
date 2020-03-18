// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.AuthzActionEnumAuthzServiceActions._
import ai.verta.swagger._public.uac.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.uac.model.EntitiesEnumEntitiesTypes._
import ai.verta.swagger._public.uac.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.uac.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.uac.model.RoleActionEnumRoleServiceActions._
import ai.verta.swagger._public.uac.model.ServiceEnumService._
import ai.verta.swagger._public.uac.model.TernaryEnumTernary._
import ai.verta.swagger._public.uac.model.UacFlagEnum._
import ai.verta.swagger._public.uac.model.UacShareViaEnum._
import ai.verta.swagger.client.objects._

case class UacGetCollaboratorResponse (
  shared_users: Option[List[UacGetCollaboratorResponse]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetCollaboratorResponse.toJson(this)
}

object UacGetCollaboratorResponse {
  def toJson(obj: UacGetCollaboratorResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.shared_users.map(x => JField("shared_users", ((x: List[UacGetCollaboratorResponse]) => JArray(x.map(((x: UacGetCollaboratorResponse) => UacGetCollaboratorResponse.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacGetCollaboratorResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacGetCollaboratorResponse(
          // TODO: handle required
          shared_users = fieldsMap.get("shared_users").map((x: JValue) => x match {case JArray(elements) => elements.map(UacGetCollaboratorResponse.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
