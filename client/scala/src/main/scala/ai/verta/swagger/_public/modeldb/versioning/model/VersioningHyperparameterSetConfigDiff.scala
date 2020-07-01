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

case class VersioningHyperparameterSetConfigDiff (
  A: Option[VersioningHyperparameterSetConfigBlob] = None,
  B: Option[VersioningHyperparameterSetConfigBlob] = None,
  C: Option[VersioningHyperparameterSetConfigBlob] = None,
  status: Option[DiffStatusEnumDiffStatus] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningHyperparameterSetConfigDiff.toJson(this)
}

object VersioningHyperparameterSetConfigDiff {
  def toJson(obj: VersioningHyperparameterSetConfigDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.A.map(x => JField("A", ((x: VersioningHyperparameterSetConfigBlob) => VersioningHyperparameterSetConfigBlob.toJson(x))(x))),
        obj.B.map(x => JField("B", ((x: VersioningHyperparameterSetConfigBlob) => VersioningHyperparameterSetConfigBlob.toJson(x))(x))),
        obj.C.map(x => JField("C", ((x: VersioningHyperparameterSetConfigBlob) => VersioningHyperparameterSetConfigBlob.toJson(x))(x))),
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningHyperparameterSetConfigDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningHyperparameterSetConfigDiff(
          // TODO: handle required
          A = fieldsMap.get("A").map(VersioningHyperparameterSetConfigBlob.fromJson),
          B = fieldsMap.get("B").map(VersioningHyperparameterSetConfigBlob.fromJson),
          C = fieldsMap.get("C").map(VersioningHyperparameterSetConfigBlob.fromJson),
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
