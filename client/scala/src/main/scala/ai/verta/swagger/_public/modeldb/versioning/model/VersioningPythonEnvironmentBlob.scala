// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningPythonEnvironmentBlob (
  version: Option[VersioningVersionEnvironmentBlob] = None,
  requirements: Option[List[VersioningPythonRequirementEnvironmentBlob]] = None,
  constraints: Option[List[VersioningPythonRequirementEnvironmentBlob]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPythonEnvironmentBlob.toJson(this)
}

object VersioningPythonEnvironmentBlob {
  def toJson(obj: VersioningPythonEnvironmentBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.version.map(x => JField("version", ((x: VersioningVersionEnvironmentBlob) => VersioningVersionEnvironmentBlob.toJson(x))(x))),
        obj.requirements.map(x => JField("requirements", ((x: List[VersioningPythonRequirementEnvironmentBlob]) => JArray(x.map(((x: VersioningPythonRequirementEnvironmentBlob) => VersioningPythonRequirementEnvironmentBlob.toJson(x)))))(x))),
        obj.constraints.map(x => JField("constraints", ((x: List[VersioningPythonRequirementEnvironmentBlob]) => JArray(x.map(((x: VersioningPythonRequirementEnvironmentBlob) => VersioningPythonRequirementEnvironmentBlob.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningPythonEnvironmentBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningPythonEnvironmentBlob(
          // TODO: handle required
          version = fieldsMap.get("version").map(VersioningVersionEnvironmentBlob.fromJson),
          requirements = fieldsMap.get("requirements").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningPythonRequirementEnvironmentBlob.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          constraints = fieldsMap.get("constraints").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningPythonRequirementEnvironmentBlob.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
