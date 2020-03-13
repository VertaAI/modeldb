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

case class ModeldbGetDatasetByNameResponse (
  dataset_by_user: Option[ModeldbDataset] = None,
  shared_datasets: Option[List[ModeldbDataset]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetDatasetByNameResponse.toJson(this)
}

object ModeldbGetDatasetByNameResponse {
  def toJson(obj: ModeldbGetDatasetByNameResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.dataset_by_user.map(x => JField("dataset_by_user", ((x: ModeldbDataset) => ModeldbDataset.toJson(x))(x))),
        obj.shared_datasets.map(x => JField("shared_datasets", ((x: List[ModeldbDataset]) => JArray(x.map(((x: ModeldbDataset) => ModeldbDataset.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetDatasetByNameResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetDatasetByNameResponse(
          // TODO: handle required
          dataset_by_user = fieldsMap.get("dataset_by_user").map(ModeldbDataset.fromJson),
          shared_datasets = fieldsMap.get("shared_datasets").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbDataset.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
