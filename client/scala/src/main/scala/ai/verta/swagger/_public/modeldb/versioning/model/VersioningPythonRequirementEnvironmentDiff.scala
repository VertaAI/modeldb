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

case class VersioningPythonRequirementEnvironmentDiff (
  A: Option[VersioningPythonRequirementEnvironmentBlob] = None,
  B: Option[VersioningPythonRequirementEnvironmentBlob] = None,
  C: Option[VersioningPythonRequirementEnvironmentBlob] = None,
  status: Option[DiffStatusEnumDiffStatus] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPythonRequirementEnvironmentDiff.toJson(this)
}

object VersioningPythonRequirementEnvironmentDiff {
  def toJson(obj: VersioningPythonRequirementEnvironmentDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.A.map(x => JField("A", ((x: VersioningPythonRequirementEnvironmentBlob) => VersioningPythonRequirementEnvironmentBlob.toJson(x))(x))),
        obj.B.map(x => JField("B", ((x: VersioningPythonRequirementEnvironmentBlob) => VersioningPythonRequirementEnvironmentBlob.toJson(x))(x))),
        obj.C.map(x => JField("C", ((x: VersioningPythonRequirementEnvironmentBlob) => VersioningPythonRequirementEnvironmentBlob.toJson(x))(x))),
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningPythonRequirementEnvironmentDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningPythonRequirementEnvironmentDiff(
          // TODO: handle required
          A = fieldsMap.get("A").map(VersioningPythonRequirementEnvironmentBlob.fromJson),
          B = fieldsMap.get("B").map(VersioningPythonRequirementEnvironmentBlob.fromJson),
          C = fieldsMap.get("C").map(VersioningPythonRequirementEnvironmentBlob.fromJson),
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
