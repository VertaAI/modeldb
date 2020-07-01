// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.versioning.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.versioning.model.RepositoryVisibilityEnumRepositoryVisibility._
import ai.verta.swagger._public.modeldb.versioning.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.versioning.model.VersioningBlobType._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningGetUrlForBlobVersioned (
  commit_sha: Option[String] = None,
  location: Option[List[String]] = None,
  method: Option[String] = None,
  part_number: Option[BigInt] = None,
  path_dataset_component_blob_path: Option[String] = None,
  repository_id: Option[VersioningRepositoryIdentification] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningGetUrlForBlobVersioned.toJson(this)
}

object VersioningGetUrlForBlobVersioned {
  def toJson(obj: VersioningGetUrlForBlobVersioned): JObject = {
    new JObject(
      List[Option[JField]](
        obj.commit_sha.map(x => JField("commit_sha", JString(x))),
        obj.location.map(x => JField("location", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.method.map(x => JField("method", JString(x))),
        obj.part_number.map(x => JField("part_number", JInt(x))),
        obj.path_dataset_component_blob_path.map(x => JField("path_dataset_component_blob_path", JString(x))),
        obj.repository_id.map(x => JField("repository_id", ((x: VersioningRepositoryIdentification) => VersioningRepositoryIdentification.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningGetUrlForBlobVersioned =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningGetUrlForBlobVersioned(
          // TODO: handle required
          commit_sha = fieldsMap.get("commit_sha").map(JsonConverter.fromJsonString),
          location = fieldsMap.get("location").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          method = fieldsMap.get("method").map(JsonConverter.fromJsonString),
          part_number = fieldsMap.get("part_number").map(JsonConverter.fromJsonInteger),
          path_dataset_component_blob_path = fieldsMap.get("path_dataset_component_blob_path").map(JsonConverter.fromJsonString),
          repository_id = fieldsMap.get("repository_id").map(VersioningRepositoryIdentification.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
