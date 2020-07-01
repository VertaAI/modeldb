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

case class VersioningNotebookCodeBlob (
  git_repo: Option[VersioningGitCodeBlob] = None,
  path: Option[VersioningPathDatasetComponentBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningNotebookCodeBlob.toJson(this)
}

object VersioningNotebookCodeBlob {
  def toJson(obj: VersioningNotebookCodeBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.git_repo.map(x => JField("git_repo", ((x: VersioningGitCodeBlob) => VersioningGitCodeBlob.toJson(x))(x))),
        obj.path.map(x => JField("path", ((x: VersioningPathDatasetComponentBlob) => VersioningPathDatasetComponentBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningNotebookCodeBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningNotebookCodeBlob(
          // TODO: handle required
          git_repo = fieldsMap.get("git_repo").map(VersioningGitCodeBlob.fromJson),
          path = fieldsMap.get("path").map(VersioningPathDatasetComponentBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
