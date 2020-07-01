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

case class VersioningCodeDiff (
  git: Option[VersioningGitCodeDiff] = None,
  notebook: Option[VersioningNotebookCodeDiff] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningCodeDiff.toJson(this)
}

object VersioningCodeDiff {
  def toJson(obj: VersioningCodeDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.git.map(x => JField("git", ((x: VersioningGitCodeDiff) => VersioningGitCodeDiff.toJson(x))(x))),
        obj.notebook.map(x => JField("notebook", ((x: VersioningNotebookCodeDiff) => VersioningNotebookCodeDiff.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningCodeDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningCodeDiff(
          // TODO: handle required
          git = fieldsMap.get("git").map(VersioningGitCodeDiff.fromJson),
          notebook = fieldsMap.get("notebook").map(VersioningNotebookCodeDiff.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
