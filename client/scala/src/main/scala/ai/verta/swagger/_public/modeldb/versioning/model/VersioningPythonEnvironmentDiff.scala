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

case class VersioningPythonEnvironmentDiff (
  constraints: Option[List[VersioningPythonRequirementEnvironmentDiff]] = None,
  requirements: Option[List[VersioningPythonRequirementEnvironmentDiff]] = None,
  version: Option[VersioningVersionEnvironmentDiff] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPythonEnvironmentDiff.toJson(this)
}

object VersioningPythonEnvironmentDiff {
  def toJson(obj: VersioningPythonEnvironmentDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.constraints.map(x => JField("constraints", ((x: List[VersioningPythonRequirementEnvironmentDiff]) => JArray(x.map(((x: VersioningPythonRequirementEnvironmentDiff) => VersioningPythonRequirementEnvironmentDiff.toJson(x)))))(x))),
        obj.requirements.map(x => JField("requirements", ((x: List[VersioningPythonRequirementEnvironmentDiff]) => JArray(x.map(((x: VersioningPythonRequirementEnvironmentDiff) => VersioningPythonRequirementEnvironmentDiff.toJson(x)))))(x))),
        obj.version.map(x => JField("version", ((x: VersioningVersionEnvironmentDiff) => VersioningVersionEnvironmentDiff.toJson(x))(x)))
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
          constraints = fieldsMap.get("constraints").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningPythonRequirementEnvironmentDiff.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          requirements = fieldsMap.get("requirements").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningPythonRequirementEnvironmentDiff.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          version = fieldsMap.get("version").map(VersioningVersionEnvironmentDiff.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
