// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningPythonEnvironmentDiff (
  A: Option[VersioningPythonEnvironmentBlob] = None,
  B: Option[VersioningPythonEnvironmentBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPythonEnvironmentDiff.toJson(this)
}

object VersioningPythonEnvironmentDiff {
  def toJson(obj: VersioningPythonEnvironmentDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.A.map(x => JField("A", ((x: VersioningPythonEnvironmentBlob) => VersioningPythonEnvironmentBlob.toJson(x))(x))),
        obj.B.map(x => JField("B", ((x: VersioningPythonEnvironmentBlob) => VersioningPythonEnvironmentBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningPythonEnvironmentDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningPythonEnvironmentDiff(
          // TODO: handle required
          A = fieldsMap.get("A").map(VersioningPythonEnvironmentBlob.fromJson),
          B = fieldsMap.get("B").map(VersioningPythonEnvironmentBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
