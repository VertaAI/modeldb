// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger.client.objects._

case class ModeldbCreateExperimentRun (
  artifacts: Option[List[CommonArtifact]] = None,
  attributes: Option[List[CommonKeyValue]] = None,
  code_version: Option[String] = None,
  code_version_snapshot: Option[ModeldbCodeVersion] = None,
  datasets: Option[List[CommonArtifact]] = None,
  date_created: Option[BigInt] = None,
  date_updated: Option[BigInt] = None,
  description: Option[String] = None,
  end_time: Option[BigInt] = None,
  experiment_id: Option[String] = None,
  features: Option[List[ModeldbFeature]] = None,
  hyperparameters: Option[List[CommonKeyValue]] = None,
  id: Option[String] = None,
  metrics: Option[List[CommonKeyValue]] = None,
  name: Option[String] = None,
  observations: Option[List[ModeldbObservation]] = None,
  parent_id: Option[String] = None,
  project_id: Option[String] = None,
  start_time: Option[BigInt] = None,
  tags: Option[List[String]] = None,
  versioned_inputs: Option[ModeldbVersioningEntry] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbCreateExperimentRun.toJson(this)
}

object ModeldbCreateExperimentRun {
  def toJson(obj: ModeldbCreateExperimentRun): JObject = {
    new JObject(
      List[Option[JField]](
        obj.artifacts.map(x => JField("artifacts", ((x: List[CommonArtifact]) => JArray(x.map(((x: CommonArtifact) => CommonArtifact.toJson(x)))))(x))),
        obj.attributes.map(x => JField("attributes", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x))),
        obj.code_version.map(x => JField("code_version", JString(x))),
        obj.code_version_snapshot.map(x => JField("code_version_snapshot", ((x: ModeldbCodeVersion) => ModeldbCodeVersion.toJson(x))(x))),
        obj.datasets.map(x => JField("datasets", ((x: List[CommonArtifact]) => JArray(x.map(((x: CommonArtifact) => CommonArtifact.toJson(x)))))(x))),
        obj.date_created.map(x => JField("date_created", JInt(x))),
        obj.date_updated.map(x => JField("date_updated", JInt(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.end_time.map(x => JField("end_time", JInt(x))),
        obj.experiment_id.map(x => JField("experiment_id", JString(x))),
        obj.features.map(x => JField("features", ((x: List[ModeldbFeature]) => JArray(x.map(((x: ModeldbFeature) => ModeldbFeature.toJson(x)))))(x))),
        obj.hyperparameters.map(x => JField("hyperparameters", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x))),
        obj.id.map(x => JField("id", JString(x))),
        obj.metrics.map(x => JField("metrics", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.observations.map(x => JField("observations", ((x: List[ModeldbObservation]) => JArray(x.map(((x: ModeldbObservation) => ModeldbObservation.toJson(x)))))(x))),
        obj.parent_id.map(x => JField("parent_id", JString(x))),
        obj.project_id.map(x => JField("project_id", JString(x))),
        obj.start_time.map(x => JField("start_time", JInt(x))),
        obj.tags.map(x => JField("tags", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.versioned_inputs.map(x => JField("versioned_inputs", ((x: ModeldbVersioningEntry) => ModeldbVersioningEntry.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbCreateExperimentRun =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbCreateExperimentRun(
          // TODO: handle required
          artifacts = fieldsMap.get("artifacts").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonArtifact.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          attributes = fieldsMap.get("attributes").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          code_version = fieldsMap.get("code_version").map(JsonConverter.fromJsonString),
          code_version_snapshot = fieldsMap.get("code_version_snapshot").map(ModeldbCodeVersion.fromJson),
          datasets = fieldsMap.get("datasets").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonArtifact.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          date_created = fieldsMap.get("date_created").map(JsonConverter.fromJsonInteger),
          date_updated = fieldsMap.get("date_updated").map(JsonConverter.fromJsonInteger),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          end_time = fieldsMap.get("end_time").map(JsonConverter.fromJsonInteger),
          experiment_id = fieldsMap.get("experiment_id").map(JsonConverter.fromJsonString),
          features = fieldsMap.get("features").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbFeature.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          hyperparameters = fieldsMap.get("hyperparameters").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          metrics = fieldsMap.get("metrics").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          observations = fieldsMap.get("observations").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbObservation.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          parent_id = fieldsMap.get("parent_id").map(JsonConverter.fromJsonString),
          project_id = fieldsMap.get("project_id").map(JsonConverter.fromJsonString),
          start_time = fieldsMap.get("start_time").map(JsonConverter.fromJsonInteger),
          tags = fieldsMap.get("tags").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          versioned_inputs = fieldsMap.get("versioned_inputs").map(ModeldbVersioningEntry.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
