// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.DatasetTypeEnumDatasetType._
import ai.verta.swagger._public.modeldb.model.DatasetVisibilityEnumDatasetVisibility._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.PathLocationTypeEnumPathLocationType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger.client.objects._

case class ModeldbGetUrlForDatasetBlobVersioned (
  dataset_id: Option[String] = None,
  dataset_version_id: Option[String] = None,
  method: Option[String] = None,
  part_number: Option[BigInt] = None,
  path_dataset_component_blob_path: Option[String] = None,
  workspace_name: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetUrlForDatasetBlobVersioned.toJson(this)
}

object ModeldbGetUrlForDatasetBlobVersioned {
  def toJson(obj: ModeldbGetUrlForDatasetBlobVersioned): JObject = {
    new JObject(
      List[Option[JField]](
        obj.dataset_id.map(x => JField("dataset_id", JString(x))),
        obj.dataset_version_id.map(x => JField("dataset_version_id", JString(x))),
        obj.method.map(x => JField("method", JString(x))),
        obj.part_number.map(x => JField("part_number", JInt(x))),
        obj.path_dataset_component_blob_path.map(x => JField("path_dataset_component_blob_path", JString(x))),
        obj.workspace_name.map(x => JField("workspace_name", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetUrlForDatasetBlobVersioned =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetUrlForDatasetBlobVersioned(
          // TODO: handle required
          dataset_id = fieldsMap.get("dataset_id").map(JsonConverter.fromJsonString),
          dataset_version_id = fieldsMap.get("dataset_version_id").map(JsonConverter.fromJsonString),
          method = fieldsMap.get("method").map(JsonConverter.fromJsonString),
          part_number = fieldsMap.get("part_number").map(JsonConverter.fromJsonInteger),
          path_dataset_component_blob_path = fieldsMap.get("path_dataset_component_blob_path").map(JsonConverter.fromJsonString),
          workspace_name = fieldsMap.get("workspace_name").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
