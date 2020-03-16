// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningDockerEnvironmentDiff (
  A: Option[VersioningDockerEnvironmentBlob] = None,
  B: Option[VersioningDockerEnvironmentBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningDockerEnvironmentDiff.toJson(this)
}

object VersioningDockerEnvironmentDiff {
  def toJson(obj: VersioningDockerEnvironmentDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.A.map(x => JField("A", ((x: VersioningDockerEnvironmentBlob) => VersioningDockerEnvironmentBlob.toJson(x))(x))),
        obj.B.map(x => JField("B", ((x: VersioningDockerEnvironmentBlob) => VersioningDockerEnvironmentBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningDockerEnvironmentDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningDockerEnvironmentDiff(
          // TODO: handle required
          A = fieldsMap.get("A").map(VersioningDockerEnvironmentBlob.fromJson),
          B = fieldsMap.get("B").map(VersioningDockerEnvironmentBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
