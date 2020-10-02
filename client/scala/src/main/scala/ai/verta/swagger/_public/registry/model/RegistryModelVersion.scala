// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.registry.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.registry.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.registry.model.OperatorEnumOperator._
import ai.verta.swagger._public.registry.model.ProtobufNullValue._
import ai.verta.swagger._public.registry.model.TernaryEnumTernary._
import ai.verta.swagger._public.registry.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.registry.model.VisibilityEnumVisibility._
import ai.verta.swagger.client.objects._

case class RegistryModelVersion (
  apis: Option[List[String]] = None,
  archived: Option[TernaryEnumTernary] = None,
  artifacts: Option[List[CommonArtifact]] = None,
  attributes: Option[List[CommonKeyValue]] = None,
  description: Option[String] = None,
  environment: Option[VersioningEnvironmentBlob] = None,
  experiment_run_id: Option[String] = None,
  id: Option[BigInt] = None,
  labels: Option[List[String]] = None,
  model: Option[CommonArtifact] = None,
  owner: Option[String] = None,
  readme_text: Option[String] = None,
  registered_model_id: Option[BigInt] = None,
  time_created: Option[BigInt] = None,
  time_updated: Option[BigInt] = None,
  version: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = RegistryModelVersion.toJson(this)
}

object RegistryModelVersion {
  def toJson(obj: RegistryModelVersion): JObject = {
    new JObject(
      List[Option[JField]](
        obj.apis.map(x => JField("apis", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.archived.map(x => JField("archived", ((x: TernaryEnumTernary) => TernaryEnumTernary.toJson(x))(x))),
        obj.artifacts.map(x => JField("artifacts", ((x: List[CommonArtifact]) => JArray(x.map(((x: CommonArtifact) => CommonArtifact.toJson(x)))))(x))),
        obj.attributes.map(x => JField("attributes", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.environment.map(x => JField("environment", ((x: VersioningEnvironmentBlob) => VersioningEnvironmentBlob.toJson(x))(x))),
        obj.experiment_run_id.map(x => JField("experiment_run_id", JString(x))),
        obj.id.map(x => JField("id", JInt(x))),
        obj.labels.map(x => JField("labels", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.model.map(x => JField("model", ((x: CommonArtifact) => CommonArtifact.toJson(x))(x))),
        obj.owner.map(x => JField("owner", JString(x))),
        obj.readme_text.map(x => JField("readme_text", JString(x))),
        obj.registered_model_id.map(x => JField("registered_model_id", JInt(x))),
        obj.time_created.map(x => JField("time_created", JInt(x))),
        obj.time_updated.map(x => JField("time_updated", JInt(x))),
        obj.version.map(x => JField("version", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RegistryModelVersion =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RegistryModelVersion(
          // TODO: handle required
          apis = fieldsMap.get("apis").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          archived = fieldsMap.get("archived").map(TernaryEnumTernary.fromJson),
          artifacts = fieldsMap.get("artifacts").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonArtifact.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          attributes = fieldsMap.get("attributes").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          environment = fieldsMap.get("environment").map(VersioningEnvironmentBlob.fromJson),
          experiment_run_id = fieldsMap.get("experiment_run_id").map(JsonConverter.fromJsonString),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonInteger),
          labels = fieldsMap.get("labels").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          model = fieldsMap.get("model").map(CommonArtifact.fromJson),
          owner = fieldsMap.get("owner").map(JsonConverter.fromJsonString),
          readme_text = fieldsMap.get("readme_text").map(JsonConverter.fromJsonString),
          registered_model_id = fieldsMap.get("registered_model_id").map(JsonConverter.fromJsonInteger),
          time_created = fieldsMap.get("time_created").map(JsonConverter.fromJsonInteger),
          time_updated = fieldsMap.get("time_updated").map(JsonConverter.fromJsonInteger),
          version = fieldsMap.get("version").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
