// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningNotebookCodeDiff (
  A: Option[VersioningNotebookCodeBlob] = None,
  B: Option[VersioningNotebookCodeBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningNotebookCodeDiff.toJson(this)
}

object VersioningNotebookCodeDiff {
  def toJson(obj: VersioningNotebookCodeDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.A.map(x => JField("A", ((x: VersioningNotebookCodeBlob) => VersioningNotebookCodeBlob.toJson(x))(x))),
        obj.B.map(x => JField("B", ((x: VersioningNotebookCodeBlob) => VersioningNotebookCodeBlob.toJson(x))(x)))
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
          A = fieldsMap.get("A").map(VersioningNotebookCodeBlob.fromJson),
          B = fieldsMap.get("B").map(VersioningNotebookCodeBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
