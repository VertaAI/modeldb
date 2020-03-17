// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbGetExperimentRunsByDatasetVersionIdResponse (
  experiment_runs: Option[List[ModeldbExperimentRun]] = None,
  total_records: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetExperimentRunsByDatasetVersionIdResponse.toJson(this)
}

object ModeldbGetExperimentRunsByDatasetVersionIdResponse {
  def toJson(obj: ModeldbGetExperimentRunsByDatasetVersionIdResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.experiment_runs.map(x => JField("experiment_runs", ((x: List[ModeldbExperimentRun]) => JArray(x.map(((x: ModeldbExperimentRun) => ModeldbExperimentRun.toJson(x)))))(x))),
        obj.total_records.map(x => JField("total_records", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetExperimentRunsByDatasetVersionIdResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetExperimentRunsByDatasetVersionIdResponse(
          // TODO: handle required
          experiment_runs = fieldsMap.get("experiment_runs").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbExperimentRun.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          total_records = fieldsMap.get("total_records").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
