// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.model.ModeldbProjectVisibility._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbGetSummaryResponse (
  name: Option[String] = None,
  last_updated_time: Option[String] = None,
  total_experiment: Option[String] = None,
  total_experiment_runs: Option[String] = None,
  last_modified_experimentRun_summary: Option[ModeldbLastModifiedExperimentRunSummary] = None,
  metrics: Option[List[ModeldbMetricsSummary]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetSummaryResponse.toJson(this)
}

object ModeldbGetSummaryResponse {
  def toJson(obj: ModeldbGetSummaryResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.name.map(x => JField("name", JString(x))),
        obj.last_updated_time.map(x => JField("last_updated_time", JString(x))),
        obj.total_experiment.map(x => JField("total_experiment", JString(x))),
        obj.total_experiment_runs.map(x => JField("total_experiment_runs", JString(x))),
        obj.last_modified_experimentRun_summary.map(x => JField("last_modified_experimentRun_summary", ((x: ModeldbLastModifiedExperimentRunSummary) => ModeldbLastModifiedExperimentRunSummary.toJson(x))(x))),
        obj.metrics.map(x => JField("metrics", ((x: List[ModeldbMetricsSummary]) => JArray(x.map(((x: ModeldbMetricsSummary) => ModeldbMetricsSummary.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetSummaryResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetSummaryResponse(
          // TODO: handle required
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          last_updated_time = fieldsMap.get("last_updated_time").map(JsonConverter.fromJsonString),
          total_experiment = fieldsMap.get("total_experiment").map(JsonConverter.fromJsonString),
          total_experiment_runs = fieldsMap.get("total_experiment_runs").map(JsonConverter.fromJsonString),
          last_modified_experimentRun_summary = fieldsMap.get("last_modified_experimentRun_summary").map(ModeldbLastModifiedExperimentRunSummary.fromJson),
          metrics = fieldsMap.get("metrics").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbMetricsSummary.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
