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

case class VersioningPythonRequirementEnvironmentBlob (
  constraint: Option[String] = None,
  library: Option[String] = None,
  version: Option[VersioningVersionEnvironmentBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPythonRequirementEnvironmentBlob.toJson(this)
}

object VersioningPythonRequirementEnvironmentBlob {
  def toJson(obj: VersioningPythonRequirementEnvironmentBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.constraint.map(x => JField("constraint", JString(x))),
        obj.library.map(x => JField("library", JString(x))),
        obj.version.map(x => JField("version", ((x: VersioningVersionEnvironmentBlob) => VersioningVersionEnvironmentBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningPythonRequirementEnvironmentBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningPythonRequirementEnvironmentBlob(
          // TODO: handle required
          constraint = fieldsMap.get("constraint").map(JsonConverter.fromJsonString),
          library = fieldsMap.get("library").map(JsonConverter.fromJsonString),
          version = fieldsMap.get("version").map(VersioningVersionEnvironmentBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
