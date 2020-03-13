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

case class ModeldbUpdateDatasetNameResponse (
  dataset: Option[ModeldbDataset] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbUpdateDatasetNameResponse.toJson(this)
}

object ModeldbUpdateDatasetNameResponse {
  def toJson(obj: ModeldbUpdateDatasetNameResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.dataset.map(x => JField("dataset", ((x: ModeldbDataset) => ModeldbDataset.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbUpdateDatasetNameResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbUpdateDatasetNameResponse(
          // TODO: handle required
          dataset = fieldsMap.get("dataset").map(ModeldbDataset.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
