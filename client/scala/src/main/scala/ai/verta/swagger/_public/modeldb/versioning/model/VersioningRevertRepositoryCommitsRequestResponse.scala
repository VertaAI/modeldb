// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.versioning.model.RepositoryVisibilityEnumRepositoryVisibility._
import ai.verta.swagger._public.modeldb.versioning.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.versioning.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.versioning.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.versioning.model.VersioningBlobType._
import ai.verta.swagger.client.objects._

case class VersioningRevertRepositoryCommitsRequestResponse (
  commit: Option[VersioningCommit] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningRevertRepositoryCommitsRequestResponse.toJson(this)
}

object VersioningRevertRepositoryCommitsRequestResponse {
  def toJson(obj: VersioningRevertRepositoryCommitsRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.commit.map(x => JField("commit", ((x: VersioningCommit) => VersioningCommit.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningRevertRepositoryCommitsRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningRevertRepositoryCommitsRequestResponse(
          // TODO: handle required
          commit = fieldsMap.get("commit").map(VersioningCommit.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
