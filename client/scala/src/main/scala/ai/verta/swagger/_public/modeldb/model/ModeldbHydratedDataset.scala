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

case class ModeldbHydratedDataset (
  dataset: Option[ModeldbDataset] = None,
  collaborator_user_infos: Option[List[ModeldbCollaboratorUserInfo]] = None,
  owner_user_info: Option[UacUserInfo] = None,
  allowed_actions: Option[List[UacAction]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbHydratedDataset.toJson(this)
}

object ModeldbHydratedDataset {
  def toJson(obj: ModeldbHydratedDataset): JObject = {
    new JObject(
      List[Option[JField]](
        obj.dataset.map(x => JField("dataset", ((x: ModeldbDataset) => ModeldbDataset.toJson(x))(x))),
        obj.collaborator_user_infos.map(x => JField("collaborator_user_infos", ((x: List[ModeldbCollaboratorUserInfo]) => JArray(x.map(((x: ModeldbCollaboratorUserInfo) => ModeldbCollaboratorUserInfo.toJson(x)))))(x))),
        obj.owner_user_info.map(x => JField("owner_user_info", ((x: UacUserInfo) => UacUserInfo.toJson(x))(x))),
        obj.allowed_actions.map(x => JField("allowed_actions", ((x: List[UacAction]) => JArray(x.map(((x: UacAction) => UacAction.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbHydratedDataset =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbHydratedDataset(
          // TODO: handle required
          dataset = fieldsMap.get("dataset").map(ModeldbDataset.fromJson),
          collaborator_user_infos = fieldsMap.get("collaborator_user_infos").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbCollaboratorUserInfo.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          owner_user_info = fieldsMap.get("owner_user_info").map(UacUserInfo.fromJson),
          allowed_actions = fieldsMap.get("allowed_actions").map((x: JValue) => x match {case JArray(elements) => elements.map(UacAction.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
