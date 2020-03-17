// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningRepository (
  id: Option[String] = None,
  name: Option[String] = None,
  date_created: Option[String] = None,
  date_updated: Option[String] = None,
  workspace_id: Option[String] = None,
  workspace_type: Option[WorkspaceTypeEnumWorkspaceType] = None,
  owner: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningRepository.toJson(this)
}

object VersioningRepository {
  def toJson(obj: VersioningRepository): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.date_created.map(x => JField("date_created", JString(x))),
        obj.date_updated.map(x => JField("date_updated", JString(x))),
        obj.workspace_id.map(x => JField("workspace_id", JString(x))),
        obj.workspace_type.map(x => JField("workspace_type", ((x: WorkspaceTypeEnumWorkspaceType) => WorkspaceTypeEnumWorkspaceType.toJson(x))(x))),
        obj.owner.map(x => JField("owner", JString(x)))
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
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          date_created = fieldsMap.get("date_created").map(JsonConverter.fromJsonString),
          date_updated = fieldsMap.get("date_updated").map(JsonConverter.fromJsonString),
          workspace_id = fieldsMap.get("workspace_id").map(JsonConverter.fromJsonString),
          workspace_type = fieldsMap.get("workspace_type").map(WorkspaceTypeEnumWorkspaceType.fromJson),
          owner = fieldsMap.get("owner").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
