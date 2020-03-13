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

case class UacAddCollaboratorRequestResponse (
  self_allowed_actions: Option[List[UacAction]] = None,
  status: Option[Boolean] = None,
  collaborator_user_info: Option[UacUserInfo] = None,
  collaborator_organization: Option[UacOrganization] = None,
  collaborator_team: Option[UacTeam] = None
) extends BaseSwagger {
  def toJson(): JValue = UacAddCollaboratorRequestResponse.toJson(this)
}

object UacAddCollaboratorRequestResponse {
  def toJson(obj: UacAddCollaboratorRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.self_allowed_actions.map(x => JField("self_allowed_actions", ((x: List[UacAction]) => JArray(x.map(((x: UacAction) => UacAction.toJson(x)))))(x))),
        obj.status.map(x => JField("status", JBool(x))),
        obj.collaborator_user_info.map(x => JField("collaborator_user_info", ((x: UacUserInfo) => UacUserInfo.toJson(x))(x))),
        obj.collaborator_organization.map(x => JField("collaborator_organization", ((x: UacOrganization) => UacOrganization.toJson(x))(x))),
        obj.collaborator_team.map(x => JField("collaborator_team", ((x: UacTeam) => UacTeam.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacAddCollaboratorRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacAddCollaboratorRequestResponse(
          // TODO: handle required
          self_allowed_actions = fieldsMap.get("self_allowed_actions").map((x: JValue) => x match {case JArray(elements) => elements.map(UacAction.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          status = fieldsMap.get("status").map(JsonConverter.fromJsonBoolean),
          collaborator_user_info = fieldsMap.get("collaborator_user_info").map(UacUserInfo.fromJson),
          collaborator_organization = fieldsMap.get("collaborator_organization").map(UacOrganization.fromJson),
          collaborator_team = fieldsMap.get("collaborator_team").map(UacTeam.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
