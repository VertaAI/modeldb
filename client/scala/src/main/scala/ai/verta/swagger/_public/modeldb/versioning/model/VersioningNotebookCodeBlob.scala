// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningNotebookCodeBlob (
  path: Option[VersioningPathDatasetBlob] = None,
  git_repo: Option[VersioningGitCodeBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningNotebookCodeBlob.toJson(this)
}

object VersioningNotebookCodeBlob {
  def toJson(obj: VersioningNotebookCodeBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.path.map(x => JField("path", ((x: VersioningPathDatasetBlob) => VersioningPathDatasetBlob.toJson(x))(x))),
        obj.git_repo.map(x => JField("git_repo", ((x: VersioningGitCodeBlob) => VersioningGitCodeBlob.toJson(x))(x)))
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
          path = fieldsMap.get("path").map(VersioningPathDatasetBlob.fromJson),
          git_repo = fieldsMap.get("git_repo").map(VersioningGitCodeBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
