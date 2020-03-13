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

case class ModeldbDataset (
  id: Option[String] = None,
  name: Option[String] = None,
  owner: Option[String] = None,
  description: Option[String] = None,
  tags: Option[List[String]] = None,
  dataset_visibility: Option[DatasetVisibilityEnumDatasetVisibility] = None,
  dataset_type: Option[DatasetTypeEnumDatasetType] = None,
  attributes: Option[List[CommonKeyValue]] = None,
  time_created: Option[String] = None,
  time_updated: Option[String] = None,
  workspace_id: Option[String] = None,
  workspace_type: Option[WorkspaceTypeEnumWorkspaceType] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbDataset.toJson(this)
}

object ModeldbDataset {
  def toJson(obj: ModeldbDataset): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.owner.map(x => JField("owner", JString(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.tags.map(x => JField("tags", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.dataset_visibility.map(x => JField("dataset_visibility", ((x: DatasetVisibilityEnumDatasetVisibility) => DatasetVisibilityEnumDatasetVisibility.toJson(x))(x))),
        obj.dataset_type.map(x => JField("dataset_type", ((x: DatasetTypeEnumDatasetType) => DatasetTypeEnumDatasetType.toJson(x))(x))),
        obj.attributes.map(x => JField("attributes", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x))),
        obj.time_created.map(x => JField("time_created", JString(x))),
        obj.time_updated.map(x => JField("time_updated", JString(x))),
        obj.workspace_id.map(x => JField("workspace_id", JString(x))),
        obj.workspace_type.map(x => JField("workspace_type", ((x: WorkspaceTypeEnumWorkspaceType) => WorkspaceTypeEnumWorkspaceType.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbDataset =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbDataset(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          owner = fieldsMap.get("owner").map(JsonConverter.fromJsonString),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          tags = fieldsMap.get("tags").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          dataset_visibility = fieldsMap.get("dataset_visibility").map(DatasetVisibilityEnumDatasetVisibility.fromJson),
          dataset_type = fieldsMap.get("dataset_type").map(DatasetTypeEnumDatasetType.fromJson),
          attributes = fieldsMap.get("attributes").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          time_created = fieldsMap.get("time_created").map(JsonConverter.fromJsonString),
          time_updated = fieldsMap.get("time_updated").map(JsonConverter.fromJsonString),
          workspace_id = fieldsMap.get("workspace_id").map(JsonConverter.fromJsonString),
          workspace_type = fieldsMap.get("workspace_type").map(WorkspaceTypeEnumWorkspaceType.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
