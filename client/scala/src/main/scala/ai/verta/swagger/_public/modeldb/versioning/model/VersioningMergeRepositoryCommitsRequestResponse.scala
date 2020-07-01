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

case class VersioningMergeRepositoryCommitsRequestResponse (
  commit: Option[VersioningCommit] = None,
  common_base: Option[VersioningCommit] = None,
  conflicts: Option[List[VersioningBlobDiff]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningMergeRepositoryCommitsRequestResponse.toJson(this)
}

object VersioningMergeRepositoryCommitsRequestResponse {
  def toJson(obj: VersioningMergeRepositoryCommitsRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.commit.map(x => JField("commit", ((x: VersioningCommit) => VersioningCommit.toJson(x))(x))),
        obj.common_base.map(x => JField("common_base", ((x: VersioningCommit) => VersioningCommit.toJson(x))(x))),
        obj.conflicts.map(x => JField("conflicts", ((x: List[VersioningBlobDiff]) => JArray(x.map(((x: VersioningBlobDiff) => VersioningBlobDiff.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningMergeRepositoryCommitsRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningMergeRepositoryCommitsRequestResponse(
          // TODO: handle required
          commit = fieldsMap.get("commit").map(VersioningCommit.fromJson),
          common_base = fieldsMap.get("common_base").map(VersioningCommit.fromJson),
          conflicts = fieldsMap.get("conflicts").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningBlobDiff.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
