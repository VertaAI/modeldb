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

case class UacRemoveCollaboratorResponse (
  status: Option[Boolean] = None,
  self_allowed_actions: Option[List[UacAction]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacRemoveCollaboratorResponse.toJson(this)
}

object UacRemoveCollaboratorResponse {
  def toJson(obj: UacRemoveCollaboratorResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.status.map(x => JField("status", JBool(x))),
        obj.self_allowed_actions.map(x => JField("self_allowed_actions", ((x: List[UacAction]) => JArray(x.map(((x: UacAction) => UacAction.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacRemoveCollaboratorResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacRemoveCollaboratorResponse(
          // TODO: handle required
          status = fieldsMap.get("status").map(JsonConverter.fromJsonBoolean),
          self_allowed_actions = fieldsMap.get("self_allowed_actions").map((x: JValue) => x match {case JArray(elements) => elements.map(UacAction.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
