// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningGetCommitComponentRequestResponse (
  folder: Option[VersioningFolder] = None,
  blob: Option[VersioningBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningGetCommitComponentRequestResponse.toJson(this)
}

object VersioningGetCommitComponentRequestResponse {
  def toJson(obj: VersioningGetCommitComponentRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.folder.map(x => JField("folder", ((x: VersioningFolder) => VersioningFolder.toJson(x))(x))),
        obj.blob.map(x => JField("blob", ((x: VersioningBlob) => VersioningBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningGetCommitComponentRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningGetCommitComponentRequestResponse(
          // TODO: handle required
          folder = fieldsMap.get("folder").map(VersioningFolder.fromJson),
          blob = fieldsMap.get("blob").map(VersioningBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
