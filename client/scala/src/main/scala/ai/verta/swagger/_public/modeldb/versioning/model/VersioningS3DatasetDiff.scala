// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningS3DatasetDiff (
  A: Option[List[VersioningS3DatasetComponentBlob]] = None,
  B: Option[List[VersioningS3DatasetComponentBlob]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningS3DatasetDiff.toJson(this)
}

object VersioningS3DatasetDiff {
  def toJson(obj: VersioningS3DatasetDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.A.map(x => JField("A", ((x: List[VersioningS3DatasetComponentBlob]) => JArray(x.map(((x: VersioningS3DatasetComponentBlob) => VersioningS3DatasetComponentBlob.toJson(x)))))(x))),
        obj.B.map(x => JField("B", ((x: List[VersioningS3DatasetComponentBlob]) => JArray(x.map(((x: VersioningS3DatasetComponentBlob) => VersioningS3DatasetComponentBlob.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningS3DatasetDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningS3DatasetDiff(
          // TODO: handle required
          A = fieldsMap.get("A").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningS3DatasetComponentBlob.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          B = fieldsMap.get("B").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningS3DatasetComponentBlob.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
