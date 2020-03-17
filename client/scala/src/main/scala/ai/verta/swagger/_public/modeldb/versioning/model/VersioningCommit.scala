// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningCommit (
  parent_shas: Option[List[String]] = None,
  message: Option[String] = None,
  date_created: Option[String] = None,
  author: Option[String] = None,
  commit_sha: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningCommit.toJson(this)
}

object VersioningCommit {
  def toJson(obj: VersioningCommit): JObject = {
    new JObject(
      List[Option[JField]](
        obj.parent_shas.map(x => JField("parent_shas", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.message.map(x => JField("message", JString(x))),
        obj.date_created.map(x => JField("date_created", JString(x))),
        obj.author.map(x => JField("author", JString(x))),
        obj.commit_sha.map(x => JField("commit_sha", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningCommit =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningCommit(
          // TODO: handle required
          parent_shas = fieldsMap.get("parent_shas").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          message = fieldsMap.get("message").map(JsonConverter.fromJsonString),
          date_created = fieldsMap.get("date_created").map(JsonConverter.fromJsonString),
          author = fieldsMap.get("author").map(JsonConverter.fromJsonString),
          commit_sha = fieldsMap.get("commit_sha").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
