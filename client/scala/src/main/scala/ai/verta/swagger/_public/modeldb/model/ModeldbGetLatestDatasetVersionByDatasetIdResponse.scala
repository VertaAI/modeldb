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

case class ModeldbGetLatestDatasetVersionByDatasetIdResponse (
  dataset_version: Option[ModeldbDatasetVersion] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetLatestDatasetVersionByDatasetIdResponse.toJson(this)
}

object ModeldbGetLatestDatasetVersionByDatasetIdResponse {
  def toJson(obj: ModeldbGetLatestDatasetVersionByDatasetIdResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.dataset_version.map(x => JField("dataset_version", ((x: ModeldbDatasetVersion) => ModeldbDatasetVersion.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetLatestDatasetVersionByDatasetIdResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetLatestDatasetVersionByDatasetIdResponse(
          // TODO: handle required
          dataset_version = fieldsMap.get("dataset_version").map(ModeldbDatasetVersion.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
