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

case class ModeldbProject (
  id: Option[String] = None,
  name: Option[String] = None,
  description: Option[String] = None,
  date_created: Option[String] = None,
  date_updated: Option[String] = None,
  short_name: Option[String] = None,
  readme_text: Option[String] = None,
  project_visibility: Option[ModeldbProjectVisibility] = None,
  workspace_id: Option[String] = None,
  workspace_type: Option[WorkspaceTypeEnumWorkspaceType] = None,
  attributes: Option[List[CommonKeyValue]] = None,
  tags: Option[List[String]] = None,
  owner: Option[String] = None,
  code_version_snapshot: Option[ModeldbCodeVersion] = None,
  artifacts: Option[List[ModeldbArtifact]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbProject.toJson(this)
}

object ModeldbProject {
  def toJson(obj: ModeldbProject): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.date_created.map(x => JField("date_created", JString(x))),
        obj.date_updated.map(x => JField("date_updated", JString(x))),
        obj.short_name.map(x => JField("short_name", JString(x))),
        obj.readme_text.map(x => JField("readme_text", JString(x))),
        obj.project_visibility.map(x => JField("project_visibility", ((x: ModeldbProjectVisibility) => ModeldbProjectVisibility.toJson(x))(x))),
        obj.workspace_id.map(x => JField("workspace_id", JString(x))),
        obj.workspace_type.map(x => JField("workspace_type", ((x: WorkspaceTypeEnumWorkspaceType) => WorkspaceTypeEnumWorkspaceType.toJson(x))(x))),
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

  def fromJson(value: JValue): ModeldbProject =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbProject(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          date_created = fieldsMap.get("date_created").map(JsonConverter.fromJsonString),
          date_updated = fieldsMap.get("date_updated").map(JsonConverter.fromJsonString),
          short_name = fieldsMap.get("short_name").map(JsonConverter.fromJsonString),
          readme_text = fieldsMap.get("readme_text").map(JsonConverter.fromJsonString),
          project_visibility = fieldsMap.get("project_visibility").map(ModeldbProjectVisibility.fromJson),
          workspace_id = fieldsMap.get("workspace_id").map(JsonConverter.fromJsonString),
          workspace_type = fieldsMap.get("workspace_type").map(WorkspaceTypeEnumWorkspaceType.fromJson),
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
