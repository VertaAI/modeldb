// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.DatasetTypeEnumDatasetType._
import ai.verta.swagger._public.modeldb.model.DatasetVisibilityEnumDatasetVisibility._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.PathLocationTypeEnumPathLocationType._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbCreateDatasetVersion (
  dataset_id: Option[String] = None,
  parent_id: Option[String] = None,
  description: Option[String] = None,
  tags: Option[List[String]] = None,
  dataset_version_visibility: Option[DatasetVisibilityEnumDatasetVisibility] = None,
  dataset_type: Option[DatasetTypeEnumDatasetType] = None,
  attributes: Option[List[CommonKeyValue]] = None,
  version: Option[String] = None,
  raw_dataset_version_info: Option[ModeldbRawDatasetVersionInfo] = None,
  path_dataset_version_info: Option[ModeldbPathDatasetVersionInfo] = None,
  query_dataset_version_info: Option[ModeldbQueryDatasetVersionInfo] = None,
  time_created: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbCreateDatasetVersion.toJson(this)
}

object ModeldbCreateDatasetVersion {
  def toJson(obj: ModeldbCreateDatasetVersion): JObject = {
    new JObject(
      List[Option[JField]](
        obj.dataset_id.map(x => JField("dataset_id", JString(x))),
        obj.parent_id.map(x => JField("parent_id", JString(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.tags.map(x => JField("tags", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.dataset_version_visibility.map(x => JField("dataset_version_visibility", ((x: DatasetVisibilityEnumDatasetVisibility) => DatasetVisibilityEnumDatasetVisibility.toJson(x))(x))),
        obj.dataset_type.map(x => JField("dataset_type", ((x: DatasetTypeEnumDatasetType) => DatasetTypeEnumDatasetType.toJson(x))(x))),
        obj.attributes.map(x => JField("attributes", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x))),
        obj.version.map(x => JField("version", JString(x))),
        obj.raw_dataset_version_info.map(x => JField("raw_dataset_version_info", ((x: ModeldbRawDatasetVersionInfo) => ModeldbRawDatasetVersionInfo.toJson(x))(x))),
        obj.path_dataset_version_info.map(x => JField("path_dataset_version_info", ((x: ModeldbPathDatasetVersionInfo) => ModeldbPathDatasetVersionInfo.toJson(x))(x))),
        obj.query_dataset_version_info.map(x => JField("query_dataset_version_info", ((x: ModeldbQueryDatasetVersionInfo) => ModeldbQueryDatasetVersionInfo.toJson(x))(x))),
        obj.time_created.map(x => JField("time_created", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbCreateDatasetVersion =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbCreateDatasetVersion(
          // TODO: handle required
          dataset_id = fieldsMap.get("dataset_id").map(JsonConverter.fromJsonString),
          parent_id = fieldsMap.get("parent_id").map(JsonConverter.fromJsonString),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          tags = fieldsMap.get("tags").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          dataset_version_visibility = fieldsMap.get("dataset_version_visibility").map(DatasetVisibilityEnumDatasetVisibility.fromJson),
          dataset_type = fieldsMap.get("dataset_type").map(DatasetTypeEnumDatasetType.fromJson),
          attributes = fieldsMap.get("attributes").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          version = fieldsMap.get("version").map(JsonConverter.fromJsonString),
          raw_dataset_version_info = fieldsMap.get("raw_dataset_version_info").map(ModeldbRawDatasetVersionInfo.fromJson),
          path_dataset_version_info = fieldsMap.get("path_dataset_version_info").map(ModeldbPathDatasetVersionInfo.fromJson),
          query_dataset_version_info = fieldsMap.get("query_dataset_version_info").map(ModeldbQueryDatasetVersionInfo.fromJson),
          time_created = fieldsMap.get("time_created").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
