// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.model.ModeldbProjectVisibility._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbCodeVersion (
  git_snapshot: Option[ModeldbGitSnapshot] = None,
  code_archive: Option[ModeldbArtifact] = None,
  date_logged: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbCodeVersion.toJson(this)
}

object ModeldbCodeVersion {
  def toJson(obj: ModeldbCodeVersion): JObject = {
    new JObject(
      List[Option[JField]](
        obj.git_snapshot.map(x => JField("git_snapshot", ((x: ModeldbGitSnapshot) => ModeldbGitSnapshot.toJson(x))(x))),
        obj.code_archive.map(x => JField("code_archive", ((x: ModeldbArtifact) => ModeldbArtifact.toJson(x))(x))),
        obj.date_logged.map(x => JField("date_logged", JString(x)))
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
          git_snapshot = fieldsMap.get("git_snapshot").map(ModeldbGitSnapshot.fromJson),
          code_archive = fieldsMap.get("code_archive").map(ModeldbArtifact.fromJson),
          date_logged = fieldsMap.get("date_logged").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
