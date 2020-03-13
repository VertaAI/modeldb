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

case class ModeldbFindDatasetVersionsResponse (
  dataset_versions: Option[List[ModeldbDatasetVersion]] = None,
  total_records: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbFindDatasetVersionsResponse.toJson(this)
}

object ModeldbFindDatasetVersionsResponse {
  def toJson(obj: ModeldbFindDatasetVersionsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.dataset_versions.map(x => JField("dataset_versions", ((x: List[ModeldbDatasetVersion]) => JArray(x.map(((x: ModeldbDatasetVersion) => ModeldbDatasetVersion.toJson(x)))))(x))),
        obj.total_records.map(x => JField("total_records", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbFindDatasetVersionsResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbFindDatasetVersionsResponse(
          // TODO: handle required
          dataset_versions = fieldsMap.get("dataset_versions").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbDatasetVersion.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          total_records = fieldsMap.get("total_records").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
