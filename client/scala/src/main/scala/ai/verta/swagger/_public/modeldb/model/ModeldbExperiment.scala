// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.AuthzActionEnumAuthzServiceActions._
import ai.verta.swagger._public.modeldb.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.modeldb.model.DatasetTypeEnumDatasetType._
import ai.verta.swagger._public.modeldb.model.DatasetVisibilityEnumDatasetVisibility._
import ai.verta.swagger._public.modeldb.model.EntitiesEnumEntitiesTypes._
import ai.verta.swagger._public.modeldb.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.modeldb.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.PathLocationTypeEnumPathLocationType._
import ai.verta.swagger._public.modeldb.model.RoleActionEnumRoleServiceActions._
import ai.verta.swagger._public.modeldb.model.ServiceEnumService._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.model.ModeldbProjectVisibility._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class ModeldbExperiment (
  id: Option[String] = None,
  project_id: Option[String] = None,
  name: Option[String] = None,
  description: Option[String] = None,
  date_created: Option[String] = None,
  date_updated: Option[String] = None,
  attributes: Option[List[CommonKeyValue]] = None,
  tags: Option[List[String]] = None,
  owner: Option[String] = None,
  code_version_snapshot: Option[ModeldbCodeVersion] = None,
  artifacts: Option[List[ModeldbArtifact]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbExperiment.toJson(this)
}

object ModeldbExperiment {
  def toJson(obj: ModeldbExperiment): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.project_id.map(x => JField("project_id", JString(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.date_created.map(x => JField("date_created", JString(x))),
        obj.date_updated.map(x => JField("date_updated", JString(x))),
        obj.attributes.map(x => JField("attributes", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x))),
        obj.tags.map(x => JField("tags", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.owner.map(x => JField("owner", JString(x))),
        obj.code_version_snapshot.map(x => JField("code_version_snapshot", ((x: ModeldbCodeVersion) => ModeldbCodeVersion.toJson(x))(x))),
        obj.artifacts.map(x => JField("artifacts", ((x: List[ModeldbArtifact]) => JArray(x.map(((x: ModeldbArtifact) => ModeldbArtifact.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbExperiment =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbExperiment(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          project_id = fieldsMap.get("project_id").map(JsonConverter.fromJsonString),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          date_created = fieldsMap.get("date_created").map(JsonConverter.fromJsonString),
          date_updated = fieldsMap.get("date_updated").map(JsonConverter.fromJsonString),
          attributes = fieldsMap.get("attributes").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          tags = fieldsMap.get("tags").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          owner = fieldsMap.get("owner").map(JsonConverter.fromJsonString),
          code_version_snapshot = fieldsMap.get("code_version_snapshot").map(ModeldbCodeVersion.fromJson),
          artifacts = fieldsMap.get("artifacts").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbArtifact.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
