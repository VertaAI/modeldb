// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.DatasetTypeEnumDatasetType._
import ai.verta.swagger._public.modeldb.model.DatasetVisibilityEnumDatasetVisibility._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbCreateDataset (
  name: Option[String] = None,
  description: Option[String] = None,
  tags: Option[List[String]] = None,
  attributes: Option[List[CommonKeyValue]] = None,
  dataset_visibility: Option[DatasetVisibilityEnumDatasetVisibility] = None,
  dataset_type: Option[DatasetTypeEnumDatasetType] = None,
  workspace_name: Option[String] = None,
  time_created: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbCreateDataset.toJson(this)
}

object ModeldbCreateDataset {
  def toJson(obj: ModeldbCreateDataset): JObject = {
    new JObject(
      List[Option[JField]](
        obj.name.map(x => JField("name", JString(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.tags.map(x => JField("tags", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.attributes.map(x => JField("attributes", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x))),
        obj.dataset_visibility.map(x => JField("dataset_visibility", ((x: DatasetVisibilityEnumDatasetVisibility) => DatasetVisibilityEnumDatasetVisibility.toJson(x))(x))),
        obj.dataset_type.map(x => JField("dataset_type", ((x: DatasetTypeEnumDatasetType) => DatasetTypeEnumDatasetType.toJson(x))(x))),
        obj.workspace_name.map(x => JField("workspace_name", JString(x))),
        obj.time_created.map(x => JField("time_created", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbCreateDataset =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbCreateDataset(
          // TODO: handle required
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          tags = fieldsMap.get("tags").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          attributes = fieldsMap.get("attributes").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          dataset_visibility = fieldsMap.get("dataset_visibility").map(DatasetVisibilityEnumDatasetVisibility.fromJson),
          dataset_type = fieldsMap.get("dataset_type").map(DatasetTypeEnumDatasetType.fromJson),
          workspace_name = fieldsMap.get("workspace_name").map(JsonConverter.fromJsonString),
          time_created = fieldsMap.get("time_created").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
