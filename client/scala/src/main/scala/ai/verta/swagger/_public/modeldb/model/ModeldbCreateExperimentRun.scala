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

case class ModeldbCreateExperimentRun (
  id: Option[String] = None,
  project_id: Option[String] = None,
  experiment_id: Option[String] = None,
  name: Option[String] = None,
  description: Option[String] = None,
  date_created: Option[String] = None,
  date_updated: Option[String] = None,
  start_time: Option[String] = None,
  end_time: Option[String] = None,
  code_version: Option[String] = None,
  code_version_snapshot: Option[ModeldbCodeVersion] = None,
  parent_id: Option[String] = None,
  tags: Option[List[String]] = None,
  attributes: Option[List[CommonKeyValue]] = None,
  hyperparameters: Option[List[CommonKeyValue]] = None,
  artifacts: Option[List[ModeldbArtifact]] = None,
  datasets: Option[List[ModeldbArtifact]] = None,
  metrics: Option[List[CommonKeyValue]] = None,
  observations: Option[List[ModeldbObservation]] = None,
  features: Option[List[ModeldbFeature]] = None,
  versioned_inputs: Option[ModeldbVersioningEntry] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbCreateExperimentRun.toJson(this)
}

object ModeldbCreateExperimentRun {
  def toJson(obj: ModeldbCreateExperimentRun): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.project_id.map(x => JField("project_id", JString(x))),
        obj.experiment_id.map(x => JField("experiment_id", JString(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.date_created.map(x => JField("date_created", JString(x))),
        obj.date_updated.map(x => JField("date_updated", JString(x))),
        obj.start_time.map(x => JField("start_time", JString(x))),
        obj.end_time.map(x => JField("end_time", JString(x))),
        obj.code_version.map(x => JField("code_version", JString(x))),
        obj.code_version_snapshot.map(x => JField("code_version_snapshot", ((x: ModeldbCodeVersion) => ModeldbCodeVersion.toJson(x))(x))),
        obj.parent_id.map(x => JField("parent_id", JString(x))),
        obj.tags.map(x => JField("tags", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.attributes.map(x => JField("attributes", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x))),
        obj.hyperparameters.map(x => JField("hyperparameters", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x))),
        obj.artifacts.map(x => JField("artifacts", ((x: List[ModeldbArtifact]) => JArray(x.map(((x: ModeldbArtifact) => ModeldbArtifact.toJson(x)))))(x))),
        obj.datasets.map(x => JField("datasets", ((x: List[ModeldbArtifact]) => JArray(x.map(((x: ModeldbArtifact) => ModeldbArtifact.toJson(x)))))(x))),
        obj.metrics.map(x => JField("metrics", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x))),
        obj.observations.map(x => JField("observations", ((x: List[ModeldbObservation]) => JArray(x.map(((x: ModeldbObservation) => ModeldbObservation.toJson(x)))))(x))),
        obj.features.map(x => JField("features", ((x: List[ModeldbFeature]) => JArray(x.map(((x: ModeldbFeature) => ModeldbFeature.toJson(x)))))(x))),
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
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          project_id = fieldsMap.get("project_id").map(JsonConverter.fromJsonString),
          experiment_id = fieldsMap.get("experiment_id").map(JsonConverter.fromJsonString),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          date_created = fieldsMap.get("date_created").map(JsonConverter.fromJsonString),
          date_updated = fieldsMap.get("date_updated").map(JsonConverter.fromJsonString),
          start_time = fieldsMap.get("start_time").map(JsonConverter.fromJsonString),
          end_time = fieldsMap.get("end_time").map(JsonConverter.fromJsonString),
          code_version = fieldsMap.get("code_version").map(JsonConverter.fromJsonString),
          code_version_snapshot = fieldsMap.get("code_version_snapshot").map(ModeldbCodeVersion.fromJson),
          parent_id = fieldsMap.get("parent_id").map(JsonConverter.fromJsonString),
          tags = fieldsMap.get("tags").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          attributes = fieldsMap.get("attributes").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          hyperparameters = fieldsMap.get("hyperparameters").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          artifacts = fieldsMap.get("artifacts").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbArtifact.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          datasets = fieldsMap.get("datasets").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbArtifact.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          metrics = fieldsMap.get("metrics").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          observations = fieldsMap.get("observations").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbObservation.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          features = fieldsMap.get("features").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbFeature.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          versioned_inputs = fieldsMap.get("versioned_inputs").map(ModeldbVersioningEntry.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
