// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningPathDatasetComponentDiff (
  status: Option[DiffStatusEnumDiffStatus] = None,
  A: Option[VersioningPathDatasetComponentBlob] = None,
  B: Option[VersioningPathDatasetComponentBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPathDatasetComponentDiff.toJson(this)
}

object VersioningPathDatasetComponentDiff {
  def toJson(obj: VersioningPathDatasetComponentDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x))),
        obj.A.map(x => JField("A", ((x: VersioningPathDatasetComponentBlob) => VersioningPathDatasetComponentBlob.toJson(x))(x))),
        obj.B.map(x => JField("B", ((x: VersioningPathDatasetComponentBlob) => VersioningPathDatasetComponentBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningPathDatasetComponentDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningPathDatasetComponentDiff(
          // TODO: handle required
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson),
          A = fieldsMap.get("A").map(VersioningPathDatasetComponentBlob.fromJson),
          B = fieldsMap.get("B").map(VersioningPathDatasetComponentBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
