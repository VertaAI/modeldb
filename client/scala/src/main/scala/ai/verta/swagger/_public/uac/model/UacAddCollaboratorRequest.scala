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

case class UacAddCollaboratorRequest (
  entity_ids: Option[List[String]] = None,
  share_with: Option[String] = None,
  collaborator_type: Option[CollaboratorTypeEnumCollaboratorType] = None,
  message: Option[String] = None,
  date_created: Option[String] = None,
  date_updated: Option[String] = None,
  can_deploy: Option[TernaryEnumTernary] = None,
  authz_entity_type: Option[EntitiesEnumEntitiesTypes] = None
) extends BaseSwagger {
  def toJson(): JValue = UacAddCollaboratorRequest.toJson(this)
}

object UacAddCollaboratorRequest {
  def toJson(obj: UacAddCollaboratorRequest): JObject = {
    new JObject(
      List[Option[JField]](
        obj.entity_ids.map(x => JField("entity_ids", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.share_with.map(x => JField("share_with", JString(x))),
        obj.collaborator_type.map(x => JField("collaborator_type", ((x: CollaboratorTypeEnumCollaboratorType) => CollaboratorTypeEnumCollaboratorType.toJson(x))(x))),
        obj.message.map(x => JField("message", JString(x))),
        obj.date_created.map(x => JField("date_created", JString(x))),
        obj.date_updated.map(x => JField("date_updated", JString(x))),
        obj.can_deploy.map(x => JField("can_deploy", ((x: TernaryEnumTernary) => TernaryEnumTernary.toJson(x))(x))),
        obj.authz_entity_type.map(x => JField("authz_entity_type", ((x: EntitiesEnumEntitiesTypes) => EntitiesEnumEntitiesTypes.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacAddCollaboratorRequest =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacAddCollaboratorRequest(
          // TODO: handle required
          entity_ids = fieldsMap.get("entity_ids").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          share_with = fieldsMap.get("share_with").map(JsonConverter.fromJsonString),
          collaborator_type = fieldsMap.get("collaborator_type").map(CollaboratorTypeEnumCollaboratorType.fromJson),
          message = fieldsMap.get("message").map(JsonConverter.fromJsonString),
          date_created = fieldsMap.get("date_created").map(JsonConverter.fromJsonString),
          date_updated = fieldsMap.get("date_updated").map(JsonConverter.fromJsonString),
          can_deploy = fieldsMap.get("can_deploy").map(TernaryEnumTernary.fromJson),
          authz_entity_type = fieldsMap.get("authz_entity_type").map(EntitiesEnumEntitiesTypes.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
