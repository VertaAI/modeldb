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

case class VersioningVersionEnvironmentDiff (
  A: Option[VersioningVersionEnvironmentBlob] = None,
  B: Option[VersioningVersionEnvironmentBlob] = None,
  C: Option[VersioningVersionEnvironmentBlob] = None,
  status: Option[DiffStatusEnumDiffStatus] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningVersionEnvironmentDiff.toJson(this)
}

object VersioningVersionEnvironmentDiff {
  def toJson(obj: VersioningVersionEnvironmentDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.A.map(x => JField("A", ((x: VersioningVersionEnvironmentBlob) => VersioningVersionEnvironmentBlob.toJson(x))(x))),
        obj.B.map(x => JField("B", ((x: VersioningVersionEnvironmentBlob) => VersioningVersionEnvironmentBlob.toJson(x))(x))),
        obj.C.map(x => JField("C", ((x: VersioningVersionEnvironmentBlob) => VersioningVersionEnvironmentBlob.toJson(x))(x))),
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningVersionEnvironmentDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningVersionEnvironmentDiff(
          // TODO: handle required
          A = fieldsMap.get("A").map(VersioningVersionEnvironmentBlob.fromJson),
          B = fieldsMap.get("B").map(VersioningVersionEnvironmentBlob.fromJson),
          C = fieldsMap.get("C").map(VersioningVersionEnvironmentBlob.fromJson),
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
