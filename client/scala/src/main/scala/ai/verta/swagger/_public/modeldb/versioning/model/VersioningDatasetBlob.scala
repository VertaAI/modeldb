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

case class VersioningDatasetBlob (
  path: Option[VersioningPathDatasetBlob] = None,
  query: Option[VersioningQueryDatasetBlob] = None,
  s3: Option[VersioningS3DatasetBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningDatasetBlob.toJson(this)
}

object VersioningDatasetBlob {
  def toJson(obj: VersioningDatasetBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.path.map(x => JField("path", ((x: VersioningPathDatasetBlob) => VersioningPathDatasetBlob.toJson(x))(x))),
        obj.query.map(x => JField("query", ((x: VersioningQueryDatasetBlob) => VersioningQueryDatasetBlob.toJson(x))(x))),
        obj.s3.map(x => JField("s3", ((x: VersioningS3DatasetBlob) => VersioningS3DatasetBlob.toJson(x))(x)))
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
          path = fieldsMap.get("path").map(VersioningPathDatasetBlob.fromJson),
          query = fieldsMap.get("query").map(VersioningQueryDatasetBlob.fromJson),
          s3 = fieldsMap.get("s3").map(VersioningS3DatasetBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
