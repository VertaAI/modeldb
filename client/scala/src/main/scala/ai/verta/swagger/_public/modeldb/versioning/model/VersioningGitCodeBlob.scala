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

case class VersioningGitCodeBlob (
  branch: Option[String] = None,
  hash: Option[String] = None,
  is_dirty: Option[Boolean] = None,
  repo: Option[String] = None,
  tag: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningGitCodeBlob.toJson(this)
}

object VersioningGitCodeBlob {
  def toJson(obj: VersioningGitCodeBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.branch.map(x => JField("branch", JString(x))),
        obj.hash.map(x => JField("hash", JString(x))),
        obj.is_dirty.map(x => JField("is_dirty", JBool(x))),
        obj.repo.map(x => JField("repo", JString(x))),
        obj.tag.map(x => JField("tag", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningGitCodeBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningGitCodeBlob(
          // TODO: handle required
          branch = fieldsMap.get("branch").map(JsonConverter.fromJsonString),
          hash = fieldsMap.get("hash").map(JsonConverter.fromJsonString),
          is_dirty = fieldsMap.get("is_dirty").map(JsonConverter.fromJsonBoolean),
          repo = fieldsMap.get("repo").map(JsonConverter.fromJsonString),
          tag = fieldsMap.get("tag").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
