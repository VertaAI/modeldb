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

case class VersioningCodeBlob (
  git: Option[VersioningGitCodeBlob] = None,
  notebook: Option[VersioningNotebookCodeBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningCodeBlob.toJson(this)
}

object VersioningCodeBlob {
  def toJson(obj: VersioningCodeBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.git.map(x => JField("git", ((x: VersioningGitCodeBlob) => VersioningGitCodeBlob.toJson(x))(x))),
        obj.notebook.map(x => JField("notebook", ((x: VersioningNotebookCodeBlob) => VersioningNotebookCodeBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningCodeBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningCodeBlob(
          // TODO: handle required
          git = fieldsMap.get("git").map(VersioningGitCodeBlob.fromJson),
          notebook = fieldsMap.get("notebook").map(VersioningNotebookCodeBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
