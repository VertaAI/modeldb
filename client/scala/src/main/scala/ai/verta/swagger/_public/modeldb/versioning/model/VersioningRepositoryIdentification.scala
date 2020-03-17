// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningRepositoryIdentification (
  named_id: Option[VersioningRepositoryNamedIdentification] = None,
  repo_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningRepositoryIdentification.toJson(this)
}

object VersioningRepositoryIdentification {
  def toJson(obj: VersioningRepositoryIdentification): JObject = {
    new JObject(
      List[Option[JField]](
        obj.named_id.map(x => JField("named_id", ((x: VersioningRepositoryNamedIdentification) => VersioningRepositoryNamedIdentification.toJson(x))(x))),
        obj.repo_id.map(x => JField("repo_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningRepositoryIdentification =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningRepositoryIdentification(
          // TODO: handle required
          named_id = fieldsMap.get("named_id").map(VersioningRepositoryNamedIdentification.fromJson),
          repo_id = fieldsMap.get("repo_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
