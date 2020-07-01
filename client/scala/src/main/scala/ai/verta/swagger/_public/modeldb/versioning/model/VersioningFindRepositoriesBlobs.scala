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

case class VersioningFindRepositoriesBlobs (
  blob_type: Option[List[VersioningBlobType]] = None,
  commits: Option[List[String]] = None,
  location_prefix: Option[List[String]] = None,
  page_limit: Option[BigInt] = None,
  page_number: Option[BigInt] = None,
  predicates: Option[List[CommonKeyValueQuery]] = None,
  repo_ids: Option[List[BigInt]] = None,
  workspace_name: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningFindRepositoriesBlobs.toJson(this)
}

object VersioningFindRepositoriesBlobs {
  def toJson(obj: VersioningFindRepositoriesBlobs): JObject = {
    new JObject(
      List[Option[JField]](
        obj.blob_type.map(x => JField("blob_type", ((x: List[VersioningBlobType]) => JArray(x.map(((x: VersioningBlobType) => VersioningBlobType.toJson(x)))))(x))),
        obj.commits.map(x => JField("commits", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.location_prefix.map(x => JField("location_prefix", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.page_limit.map(x => JField("page_limit", JInt(x))),
        obj.page_number.map(x => JField("page_number", JInt(x))),
        obj.predicates.map(x => JField("predicates", ((x: List[CommonKeyValueQuery]) => JArray(x.map(((x: CommonKeyValueQuery) => CommonKeyValueQuery.toJson(x)))))(x))),
        obj.repo_ids.map(x => JField("repo_ids", ((x: List[BigInt]) => JArray(x.map(JInt)))(x))),
        obj.workspace_name.map(x => JField("workspace_name", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningFindRepositoriesBlobs =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningFindRepositoriesBlobs(
          // TODO: handle required
          blob_type = fieldsMap.get("blob_type").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningBlobType.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          commits = fieldsMap.get("commits").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          location_prefix = fieldsMap.get("location_prefix").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          page_limit = fieldsMap.get("page_limit").map(JsonConverter.fromJsonInteger),
          page_number = fieldsMap.get("page_number").map(JsonConverter.fromJsonInteger),
          predicates = fieldsMap.get("predicates").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValueQuery.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          repo_ids = fieldsMap.get("repo_ids").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonInteger); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          workspace_name = fieldsMap.get("workspace_name").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
