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

case class VersioningS3DatasetComponentDiff (
  A: Option[VersioningS3DatasetComponentBlob] = None,
  B: Option[VersioningS3DatasetComponentBlob] = None,
  C: Option[VersioningS3DatasetComponentBlob] = None,
  status: Option[DiffStatusEnumDiffStatus] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningS3DatasetComponentDiff.toJson(this)
}

object VersioningS3DatasetComponentDiff {
  def toJson(obj: VersioningS3DatasetComponentDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.A.map(x => JField("A", ((x: VersioningS3DatasetComponentBlob) => VersioningS3DatasetComponentBlob.toJson(x))(x))),
        obj.B.map(x => JField("B", ((x: VersioningS3DatasetComponentBlob) => VersioningS3DatasetComponentBlob.toJson(x))(x))),
        obj.C.map(x => JField("C", ((x: VersioningS3DatasetComponentBlob) => VersioningS3DatasetComponentBlob.toJson(x))(x))),
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningS3DatasetComponentDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningS3DatasetComponentDiff(
          // TODO: handle required
          A = fieldsMap.get("A").map(VersioningS3DatasetComponentBlob.fromJson),
          B = fieldsMap.get("B").map(VersioningS3DatasetComponentBlob.fromJson),
          C = fieldsMap.get("C").map(VersioningS3DatasetComponentBlob.fromJson),
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
