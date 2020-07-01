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

case class VersioningDockerEnvironmentBlob (
  repository: Option[String] = None,
  sha: Option[String] = None,
  tag: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningDockerEnvironmentBlob.toJson(this)
}

object VersioningDockerEnvironmentBlob {
  def toJson(obj: VersioningDockerEnvironmentBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.repository.map(x => JField("repository", JString(x))),
        obj.sha.map(x => JField("sha", JString(x))),
        obj.tag.map(x => JField("tag", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningDockerEnvironmentBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningDockerEnvironmentBlob(
          // TODO: handle required
          repository = fieldsMap.get("repository").map(JsonConverter.fromJsonString),
          sha = fieldsMap.get("sha").map(JsonConverter.fromJsonString),
          tag = fieldsMap.get("tag").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
