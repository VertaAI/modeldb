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

case class VersioningMergeRepositoryCommitsRequest (
  branch_a: Option[String] = None,
  branch_b: Option[String] = None,
  commit_sha_a: Option[String] = None,
  commit_sha_b: Option[String] = None,
  content: Option[VersioningCommit] = None,
  is_dry_run: Option[Boolean] = None,
  repository_id: Option[VersioningRepositoryIdentification] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningMergeRepositoryCommitsRequest.toJson(this)
}

object VersioningMergeRepositoryCommitsRequest {
  def toJson(obj: VersioningMergeRepositoryCommitsRequest): JObject = {
    new JObject(
      List[Option[JField]](
        obj.branch_a.map(x => JField("branch_a", JString(x))),
        obj.branch_b.map(x => JField("branch_b", JString(x))),
        obj.commit_sha_a.map(x => JField("commit_sha_a", JString(x))),
        obj.commit_sha_b.map(x => JField("commit_sha_b", JString(x))),
        obj.content.map(x => JField("content", ((x: VersioningCommit) => VersioningCommit.toJson(x))(x))),
        obj.is_dry_run.map(x => JField("is_dry_run", JBool(x))),
        obj.repository_id.map(x => JField("repository_id", ((x: VersioningRepositoryIdentification) => VersioningRepositoryIdentification.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningMergeRepositoryCommitsRequest =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningMergeRepositoryCommitsRequest(
          // TODO: handle required
          branch_a = fieldsMap.get("branch_a").map(JsonConverter.fromJsonString),
          branch_b = fieldsMap.get("branch_b").map(JsonConverter.fromJsonString),
          commit_sha_a = fieldsMap.get("commit_sha_a").map(JsonConverter.fromJsonString),
          commit_sha_b = fieldsMap.get("commit_sha_b").map(JsonConverter.fromJsonString),
          content = fieldsMap.get("content").map(VersioningCommit.fromJson),
          is_dry_run = fieldsMap.get("is_dry_run").map(JsonConverter.fromJsonBoolean),
          repository_id = fieldsMap.get("repository_id").map(VersioningRepositoryIdentification.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
