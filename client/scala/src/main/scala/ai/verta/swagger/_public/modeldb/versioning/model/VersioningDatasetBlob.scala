// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningDatasetBlob (
  s3: Option[VersioningS3DatasetBlob] = None,
  path: Option[VersioningPathDatasetBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningDatasetBlob.toJson(this)
}

object VersioningDatasetBlob {
  def toJson(obj: VersioningDatasetBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.s3.map(x => JField("s3", ((x: VersioningS3DatasetBlob) => VersioningS3DatasetBlob.toJson(x))(x))),
        obj.path.map(x => JField("path", ((x: VersioningPathDatasetBlob) => VersioningPathDatasetBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningDatasetBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningDatasetBlob(
          // TODO: handle required
          s3 = fieldsMap.get("s3").map(VersioningS3DatasetBlob.fromJson),
          path = fieldsMap.get("path").map(VersioningPathDatasetBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
