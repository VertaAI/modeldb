// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningListBranchCommitsRequestResponse (
  commits: Option[List[VersioningCommit]] = None,
  total_records: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningListBranchCommitsRequestResponse.toJson(this)
}

object VersioningListBranchCommitsRequestResponse {
  def toJson(obj: VersioningListBranchCommitsRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.commits.map(x => JField("commits", ((x: List[VersioningCommit]) => JArray(x.map(((x: VersioningCommit) => VersioningCommit.toJson(x)))))(x))),
        obj.total_records.map(x => JField("total_records", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningListBranchCommitsRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningListBranchCommitsRequestResponse(
          // TODO: handle required
          commits = fieldsMap.get("commits").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningCommit.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          total_records = fieldsMap.get("total_records").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
