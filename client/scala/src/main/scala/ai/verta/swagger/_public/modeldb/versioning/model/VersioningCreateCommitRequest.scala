// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningCreateCommitRequest (
  repository_id: Option[VersioningRepositoryIdentification] = None,
  commit: Option[VersioningCommit] = None,
  blobs: Option[List[VersioningBlobExpanded]] = None,
  commit_base: Option[String] = None,
  diffs: Option[List[VersioningBlobDiff]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningCreateCommitRequest.toJson(this)
}

object VersioningCreateCommitRequest {
  def toJson(obj: VersioningCreateCommitRequest): JObject = {
    new JObject(
      List[Option[JField]](
        obj.repository_id.map(x => JField("repository_id", ((x: VersioningRepositoryIdentification) => VersioningRepositoryIdentification.toJson(x))(x))),
        obj.commit.map(x => JField("commit", ((x: VersioningCommit) => VersioningCommit.toJson(x))(x))),
        obj.blobs.map(x => JField("blobs", ((x: List[VersioningBlobExpanded]) => JArray(x.map(((x: VersioningBlobExpanded) => VersioningBlobExpanded.toJson(x)))))(x))),
        obj.commit_base.map(x => JField("commit_base", JString(x))),
        obj.diffs.map(x => JField("diffs", ((x: List[VersioningBlobDiff]) => JArray(x.map(((x: VersioningBlobDiff) => VersioningBlobDiff.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningCreateCommitRequest =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningCreateCommitRequest(
          // TODO: handle required
          repository_id = fieldsMap.get("repository_id").map(VersioningRepositoryIdentification.fromJson),
          commit = fieldsMap.get("commit").map(VersioningCommit.fromJson),
          blobs = fieldsMap.get("blobs").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningBlobExpanded.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          commit_base = fieldsMap.get("commit_base").map(JsonConverter.fromJsonString),
          diffs = fieldsMap.get("diffs").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningBlobDiff.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
