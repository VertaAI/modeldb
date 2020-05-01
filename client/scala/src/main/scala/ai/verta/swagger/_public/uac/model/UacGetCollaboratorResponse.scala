// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.uac.model.EntitiesEnumEntitiesTypes._
import ai.verta.swagger._public.uac.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.uac.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.uac.model.ServiceEnumService._
import ai.verta.swagger._public.uac.model.TernaryEnumTernary._
import ai.verta.swagger._public.uac.model.UacFlagEnum._
import ai.verta.swagger._public.uac.model.UacShareViaEnum._
import ai.verta.swagger.client.objects._

case class UacGetCollaboratorResponse (
  authz_entity_type: Option[EntitiesEnumEntitiesTypes] = None,
  can_deploy: Option[TernaryEnumTernary] = None,
  collaborator_type: Option[CollaboratorTypeEnumCollaboratorType] = None,
  share_via_type: Option[UacShareViaEnum] = None,
  user_id: Option[String] = None,
  verta_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetCollaboratorResponse.toJson(this)
}

object UacGetCollaboratorResponse {
  def toJson(obj: UacGetCollaboratorResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.authz_entity_type.map(x => JField("authz_entity_type", ((x: EntitiesEnumEntitiesTypes) => EntitiesEnumEntitiesTypes.toJson(x))(x))),
        obj.can_deploy.map(x => JField("can_deploy", ((x: TernaryEnumTernary) => TernaryEnumTernary.toJson(x))(x))),
        obj.collaborator_type.map(x => JField("collaborator_type", ((x: CollaboratorTypeEnumCollaboratorType) => CollaboratorTypeEnumCollaboratorType.toJson(x))(x))),
        obj.share_via_type.map(x => JField("share_via_type", ((x: UacShareViaEnum) => UacShareViaEnum.toJson(x))(x))),
        obj.user_id.map(x => JField("user_id", JString(x))),
        obj.verta_id.map(x => JField("verta_id", JString(x)))
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
          authz_entity_type = fieldsMap.get("authz_entity_type").map(EntitiesEnumEntitiesTypes.fromJson),
          can_deploy = fieldsMap.get("can_deploy").map(TernaryEnumTernary.fromJson),
          collaborator_type = fieldsMap.get("collaborator_type").map(CollaboratorTypeEnumCollaboratorType.fromJson),
          share_via_type = fieldsMap.get("share_via_type").map(UacShareViaEnum.fromJson),
          user_id = fieldsMap.get("user_id").map(JsonConverter.fromJsonString),
          verta_id = fieldsMap.get("verta_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
