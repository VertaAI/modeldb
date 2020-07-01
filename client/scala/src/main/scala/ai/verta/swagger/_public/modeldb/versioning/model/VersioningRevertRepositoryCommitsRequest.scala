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

case class VersioningRevertRepositoryCommitsRequest (
  base_commit_sha: Option[String] = None,
  commit_to_revert_sha: Option[String] = None,
  content: Option[VersioningCommit] = None,
  repository_id: Option[VersioningRepositoryIdentification] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningRevertRepositoryCommitsRequest.toJson(this)
}

object VersioningRevertRepositoryCommitsRequest {
  def toJson(obj: VersioningRevertRepositoryCommitsRequest): JObject = {
    new JObject(
      List[Option[JField]](
        obj.base_commit_sha.map(x => JField("base_commit_sha", JString(x))),
        obj.commit_to_revert_sha.map(x => JField("commit_to_revert_sha", JString(x))),
        obj.content.map(x => JField("content", ((x: VersioningCommit) => VersioningCommit.toJson(x))(x))),
        obj.repository_id.map(x => JField("repository_id", ((x: VersioningRepositoryIdentification) => VersioningRepositoryIdentification.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningRevertRepositoryCommitsRequest =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningRevertRepositoryCommitsRequest(
          // TODO: handle required
          base_commit_sha = fieldsMap.get("base_commit_sha").map(JsonConverter.fromJsonString),
          commit_to_revert_sha = fieldsMap.get("commit_to_revert_sha").map(JsonConverter.fromJsonString),
          content = fieldsMap.get("content").map(VersioningCommit.fromJson),
          repository_id = fieldsMap.get("repository_id").map(VersioningRepositoryIdentification.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
