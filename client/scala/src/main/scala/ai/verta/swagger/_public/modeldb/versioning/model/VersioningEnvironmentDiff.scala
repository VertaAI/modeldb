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

case class VersioningEnvironmentDiff (
  command_line: Option[VersioningCommandLineEnvironmentDiff] = None,
  docker: Option[VersioningDockerEnvironmentDiff] = None,
  environment_variables: Option[List[VersioningEnvironmentVariablesDiff]] = None,
  python: Option[VersioningPythonEnvironmentDiff] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningEnvironmentDiff.toJson(this)
}

object VersioningEnvironmentDiff {
  def toJson(obj: VersioningEnvironmentDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.command_line.map(x => JField("command_line", ((x: VersioningCommandLineEnvironmentDiff) => VersioningCommandLineEnvironmentDiff.toJson(x))(x))),
        obj.docker.map(x => JField("docker", ((x: VersioningDockerEnvironmentDiff) => VersioningDockerEnvironmentDiff.toJson(x))(x))),
        obj.environment_variables.map(x => JField("environment_variables", ((x: List[VersioningEnvironmentVariablesDiff]) => JArray(x.map(((x: VersioningEnvironmentVariablesDiff) => VersioningEnvironmentVariablesDiff.toJson(x)))))(x))),
        obj.python.map(x => JField("python", ((x: VersioningPythonEnvironmentDiff) => VersioningPythonEnvironmentDiff.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningEnvironmentDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningEnvironmentDiff(
          // TODO: handle required
          command_line = fieldsMap.get("command_line").map(VersioningCommandLineEnvironmentDiff.fromJson),
          docker = fieldsMap.get("docker").map(VersioningDockerEnvironmentDiff.fromJson),
          environment_variables = fieldsMap.get("environment_variables").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningEnvironmentVariablesDiff.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          python = fieldsMap.get("python").map(VersioningPythonEnvironmentDiff.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
