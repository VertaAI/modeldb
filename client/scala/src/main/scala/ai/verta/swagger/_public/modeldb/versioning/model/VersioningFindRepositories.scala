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

case class VersioningFindRepositories (
  page_limit: Option[BigInt] = None,
  page_number: Option[BigInt] = None,
  predicates: Option[List[CommonKeyValueQuery]] = None,
  repo_ids: Option[List[BigInt]] = None,
  workspace_name: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningFindRepositories.toJson(this)
}

object VersioningFindRepositories {
  def toJson(obj: VersioningFindRepositories): JObject = {
    new JObject(
      List[Option[JField]](
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

  def fromJson(value: JValue): VersioningFindRepositories =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningFindRepositories(
          // TODO: handle required
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
