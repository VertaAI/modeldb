// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.AuthzActionEnumAuthzServiceActions._
import ai.verta.swagger._public.modeldb.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.modeldb.model.DatasetTypeEnumDatasetType._
import ai.verta.swagger._public.modeldb.model.DatasetVisibilityEnumDatasetVisibility._
import ai.verta.swagger._public.modeldb.model.EntitiesEnumEntitiesTypes._
import ai.verta.swagger._public.modeldb.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.modeldb.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.PathLocationTypeEnumPathLocationType._
import ai.verta.swagger._public.modeldb.model.RoleActionEnumRoleServiceActions._
import ai.verta.swagger._public.modeldb.model.ServiceEnumService._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.model.ModeldbProjectVisibility._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class ModeldbCollaboratorUserInfo (
  collaborator_user_info: Option[UacUserInfo] = None,
  collaborator_organization: Option[UacOrganization] = None,
  collaborator_team: Option[UacTeam] = None,
  collaborator_type: Option[CollaboratorTypeEnumCollaboratorType] = None,
  can_deploy: Option[TernaryEnumTernary] = None,
  entity_type: Option[EntitiesEnumEntitiesTypes] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbCollaboratorUserInfo.toJson(this)
}

object ModeldbCollaboratorUserInfo {
  def toJson(obj: ModeldbCollaboratorUserInfo): JObject = {
    new JObject(
      List[Option[JField]](
        obj.collaborator_user_info.map(x => JField("collaborator_user_info", ((x: UacUserInfo) => UacUserInfo.toJson(x))(x))),
        obj.collaborator_organization.map(x => JField("collaborator_organization", ((x: UacOrganization) => UacOrganization.toJson(x))(x))),
        obj.collaborator_team.map(x => JField("collaborator_team", ((x: UacTeam) => UacTeam.toJson(x))(x))),
        obj.collaborator_type.map(x => JField("collaborator_type", ((x: CollaboratorTypeEnumCollaboratorType) => CollaboratorTypeEnumCollaboratorType.toJson(x))(x))),
        obj.can_deploy.map(x => JField("can_deploy", ((x: TernaryEnumTernary) => TernaryEnumTernary.toJson(x))(x))),
        obj.entity_type.map(x => JField("entity_type", ((x: EntitiesEnumEntitiesTypes) => EntitiesEnumEntitiesTypes.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbCollaboratorUserInfo =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbCollaboratorUserInfo(
          // TODO: handle required
          collaborator_user_info = fieldsMap.get("collaborator_user_info").map(UacUserInfo.fromJson),
          collaborator_organization = fieldsMap.get("collaborator_organization").map(UacOrganization.fromJson),
          collaborator_team = fieldsMap.get("collaborator_team").map(UacTeam.fromJson),
          collaborator_type = fieldsMap.get("collaborator_type").map(CollaboratorTypeEnumCollaboratorType.fromJson),
          can_deploy = fieldsMap.get("can_deploy").map(TernaryEnumTernary.fromJson),
          entity_type = fieldsMap.get("entity_type").map(EntitiesEnumEntitiesTypes.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
