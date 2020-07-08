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

case class ModeldbCodeVersion (
  code_archive: Option[CommonArtifact] = None,
  date_logged: Option[BigInt] = None,
  git_snapshot: Option[ModeldbGitSnapshot] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbCodeVersion.toJson(this)
}

object ModeldbCodeVersion {
  def toJson(obj: ModeldbCodeVersion): JObject = {
    new JObject(
      List[Option[JField]](
        obj.code_archive.map(x => JField("code_archive", ((x: CommonArtifact) => CommonArtifact.toJson(x))(x))),
        obj.date_logged.map(x => JField("date_logged", JInt(x))),
        obj.git_snapshot.map(x => JField("git_snapshot", ((x: ModeldbGitSnapshot) => ModeldbGitSnapshot.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbCodeVersion =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbCodeVersion(
          // TODO: handle required
          code_archive = fieldsMap.get("code_archive").map(CommonArtifact.fromJson),
          date_logged = fieldsMap.get("date_logged").map(JsonConverter.fromJsonInteger),
          git_snapshot = fieldsMap.get("git_snapshot").map(ModeldbGitSnapshot.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
