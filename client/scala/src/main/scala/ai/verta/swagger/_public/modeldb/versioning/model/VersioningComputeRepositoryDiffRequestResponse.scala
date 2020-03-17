// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningComputeRepositoryDiffRequestResponse (
  diffs: Option[List[VersioningBlobDiff]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningComputeRepositoryDiffRequestResponse.toJson(this)
}

object VersioningComputeRepositoryDiffRequestResponse {
  def toJson(obj: VersioningComputeRepositoryDiffRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.diffs.map(x => JField("diffs", ((x: List[VersioningBlobDiff]) => JArray(x.map(((x: VersioningBlobDiff) => VersioningBlobDiff.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningComputeRepositoryDiffRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningComputeRepositoryDiffRequestResponse(
          // TODO: handle required
          diffs = fieldsMap.get("diffs").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningBlobDiff.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
