// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningEnvironmentBlob (
  python: Option[VersioningPythonEnvironmentBlob] = None,
  docker: Option[VersioningDockerEnvironmentBlob] = None,
  environment_variables: Option[List[VersioningEnvironmentVariablesBlob]] = None,
  command_line: Option[List[String]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningEnvironmentBlob.toJson(this)
}

object VersioningEnvironmentBlob {
  def toJson(obj: VersioningEnvironmentBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.python.map(x => JField("python", ((x: VersioningPythonEnvironmentBlob) => VersioningPythonEnvironmentBlob.toJson(x))(x))),
        obj.docker.map(x => JField("docker", ((x: VersioningDockerEnvironmentBlob) => VersioningDockerEnvironmentBlob.toJson(x))(x))),
        obj.environment_variables.map(x => JField("environment_variables", ((x: List[VersioningEnvironmentVariablesBlob]) => JArray(x.map(((x: VersioningEnvironmentVariablesBlob) => VersioningEnvironmentVariablesBlob.toJson(x)))))(x))),
        obj.command_line.map(x => JField("command_line", ((x: List[String]) => JArray(x.map(JString)))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningEnvironmentBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningEnvironmentBlob(
          // TODO: handle required
          python = fieldsMap.get("python").map(VersioningPythonEnvironmentBlob.fromJson),
          docker = fieldsMap.get("docker").map(VersioningDockerEnvironmentBlob.fromJson),
          environment_variables = fieldsMap.get("environment_variables").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningEnvironmentVariablesBlob.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          command_line = fieldsMap.get("command_line").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
