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

case class VersioningQueryDatasetComponentDiff (
  A: Option[VersioningQueryDatasetComponentBlob] = None,
  B: Option[VersioningQueryDatasetComponentBlob] = None,
  C: Option[VersioningQueryDatasetComponentBlob] = None,
  status: Option[DiffStatusEnumDiffStatus] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningQueryDatasetComponentDiff.toJson(this)
}

object VersioningQueryDatasetComponentDiff {
  def toJson(obj: VersioningQueryDatasetComponentDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.A.map(x => JField("A", ((x: VersioningQueryDatasetComponentBlob) => VersioningQueryDatasetComponentBlob.toJson(x))(x))),
        obj.B.map(x => JField("B", ((x: VersioningQueryDatasetComponentBlob) => VersioningQueryDatasetComponentBlob.toJson(x))(x))),
        obj.C.map(x => JField("C", ((x: VersioningQueryDatasetComponentBlob) => VersioningQueryDatasetComponentBlob.toJson(x))(x))),
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningQueryDatasetComponentDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningQueryDatasetComponentDiff(
          // TODO: handle required
          A = fieldsMap.get("A").map(VersioningQueryDatasetComponentBlob.fromJson),
          B = fieldsMap.get("B").map(VersioningQueryDatasetComponentBlob.fromJson),
          C = fieldsMap.get("C").map(VersioningQueryDatasetComponentBlob.fromJson),
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
