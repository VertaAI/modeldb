// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningNotebookCodeDiff (
  path: Option[VersioningPathDatasetComponentDiff] = None,
  git_repo: Option[VersioningGitCodeDiff] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningNotebookCodeDiff.toJson(this)
}

object VersioningNotebookCodeDiff {
  def toJson(obj: VersioningNotebookCodeDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.path.map(x => JField("path", ((x: VersioningPathDatasetComponentDiff) => VersioningPathDatasetComponentDiff.toJson(x))(x))),
        obj.git_repo.map(x => JField("git_repo", ((x: VersioningGitCodeDiff) => VersioningGitCodeDiff.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningNotebookCodeDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningNotebookCodeDiff(
          // TODO: handle required
          path = fieldsMap.get("path").map(VersioningPathDatasetComponentDiff.fromJson),
          git_repo = fieldsMap.get("git_repo").map(VersioningGitCodeDiff.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
