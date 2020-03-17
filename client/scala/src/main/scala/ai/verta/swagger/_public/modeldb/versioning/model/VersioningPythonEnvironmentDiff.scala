// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningPythonEnvironmentDiff (
  version_status: Option[DiffStatusEnumDiffStatus] = None,
  version_a: Option[VersioningVersionEnvironmentBlob] = None,
  version_b: Option[VersioningVersionEnvironmentBlob] = None,
  requirements: Option[List[VersioningPythonRequirementEnvironmentDiff]] = None,
  constraints: Option[List[VersioningPythonRequirementEnvironmentDiff]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPythonEnvironmentDiff.toJson(this)
}

object VersioningPythonEnvironmentDiff {
  def toJson(obj: VersioningPythonEnvironmentDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.version_status.map(x => JField("version_status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x))),
        obj.version_a.map(x => JField("version_a", ((x: VersioningVersionEnvironmentBlob) => VersioningVersionEnvironmentBlob.toJson(x))(x))),
        obj.version_b.map(x => JField("version_b", ((x: VersioningVersionEnvironmentBlob) => VersioningVersionEnvironmentBlob.toJson(x))(x))),
        obj.requirements.map(x => JField("requirements", ((x: List[VersioningPythonRequirementEnvironmentDiff]) => JArray(x.map(((x: VersioningPythonRequirementEnvironmentDiff) => VersioningPythonRequirementEnvironmentDiff.toJson(x)))))(x))),
        obj.constraints.map(x => JField("constraints", ((x: List[VersioningPythonRequirementEnvironmentDiff]) => JArray(x.map(((x: VersioningPythonRequirementEnvironmentDiff) => VersioningPythonRequirementEnvironmentDiff.toJson(x)))))(x)))
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
          version_status = fieldsMap.get("version_status").map(DiffStatusEnumDiffStatus.fromJson),
          version_a = fieldsMap.get("version_a").map(VersioningVersionEnvironmentBlob.fromJson),
          version_b = fieldsMap.get("version_b").map(VersioningVersionEnvironmentBlob.fromJson),
          requirements = fieldsMap.get("requirements").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningPythonRequirementEnvironmentDiff.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          constraints = fieldsMap.get("constraints").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningPythonRequirementEnvironmentDiff.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
