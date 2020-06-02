// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.versioning.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.versioning.model.RepositoryVisibilityEnumRepositoryVisibility._
import ai.verta.swagger._public.modeldb.versioning.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.versioning.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.versioning.model.VersioningBlobType._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningRepository (
  date_created: Option[BigInt] = None,
  date_updated: Option[BigInt] = None,
  id: Option[BigInt] = None,
  name: Option[String] = None,
  owner: Option[String] = None,
  repository_visibility: Option[RepositoryVisibilityEnumRepositoryVisibility] = None,
  workspace_id: Option[String] = None,
  workspace_type: Option[WorkspaceTypeEnumWorkspaceType] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningRepository.toJson(this)
}

object VersioningRepository {
  def toJson(obj: VersioningRepository): JObject = {
    new JObject(
      List[Option[JField]](
        obj.date_created.map(x => JField("date_created", JInt(x))),
        obj.date_updated.map(x => JField("date_updated", JInt(x))),
        obj.id.map(x => JField("id", JInt(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.owner.map(x => JField("owner", JString(x))),
        obj.repository_visibility.map(x => JField("repository_visibility", ((x: RepositoryVisibilityEnumRepositoryVisibility) => RepositoryVisibilityEnumRepositoryVisibility.toJson(x))(x))),
        obj.workspace_id.map(x => JField("workspace_id", JString(x))),
        obj.workspace_type.map(x => JField("workspace_type", ((x: WorkspaceTypeEnumWorkspaceType) => WorkspaceTypeEnumWorkspaceType.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningRepository =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningRepository(
          // TODO: handle required
          date_created = fieldsMap.get("date_created").map(JsonConverter.fromJsonInteger),
          date_updated = fieldsMap.get("date_updated").map(JsonConverter.fromJsonInteger),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonInteger),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          owner = fieldsMap.get("owner").map(JsonConverter.fromJsonString),
          repository_visibility = fieldsMap.get("repository_visibility").map(RepositoryVisibilityEnumRepositoryVisibility.fromJson),
          workspace_id = fieldsMap.get("workspace_id").map(JsonConverter.fromJsonString),
          workspace_type = fieldsMap.get("workspace_type").map(WorkspaceTypeEnumWorkspaceType.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
