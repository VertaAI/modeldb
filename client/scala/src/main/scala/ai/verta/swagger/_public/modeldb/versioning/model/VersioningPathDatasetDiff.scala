// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningPathDatasetDiff (
  A: Option[List[VersioningPathDatasetComponentBlob]] = None,
  B: Option[List[VersioningPathDatasetComponentBlob]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPathDatasetDiff.toJson(this)
}

object VersioningPathDatasetDiff {
  def toJson(obj: VersioningPathDatasetDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.A.map(x => JField("A", ((x: List[VersioningPathDatasetComponentBlob]) => JArray(x.map(((x: VersioningPathDatasetComponentBlob) => VersioningPathDatasetComponentBlob.toJson(x)))))(x))),
        obj.B.map(x => JField("B", ((x: List[VersioningPathDatasetComponentBlob]) => JArray(x.map(((x: VersioningPathDatasetComponentBlob) => VersioningPathDatasetComponentBlob.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningPathDatasetDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningPathDatasetDiff(
          // TODO: handle required
          A = fieldsMap.get("A").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningPathDatasetComponentBlob.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          B = fieldsMap.get("B").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningPathDatasetComponentBlob.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
