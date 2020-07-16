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

case class VersioningGitCodeDiff (
  A: Option[VersioningGitCodeBlob] = None,
  B: Option[VersioningGitCodeBlob] = None,
  C: Option[VersioningGitCodeBlob] = None,
  status: Option[DiffStatusEnumDiffStatus] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningGitCodeDiff.toJson(this)
}

object VersioningGitCodeDiff {
  def toJson(obj: VersioningGitCodeDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.A.map(x => JField("A", ((x: VersioningGitCodeBlob) => VersioningGitCodeBlob.toJson(x))(x))),
        obj.B.map(x => JField("B", ((x: VersioningGitCodeBlob) => VersioningGitCodeBlob.toJson(x))(x))),
        obj.C.map(x => JField("C", ((x: VersioningGitCodeBlob) => VersioningGitCodeBlob.toJson(x))(x))),
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningGitCodeDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningGitCodeDiff(
          // TODO: handle required
          A = fieldsMap.get("A").map(VersioningGitCodeBlob.fromJson),
          B = fieldsMap.get("B").map(VersioningGitCodeBlob.fromJson),
          C = fieldsMap.get("C").map(VersioningGitCodeBlob.fromJson),
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
