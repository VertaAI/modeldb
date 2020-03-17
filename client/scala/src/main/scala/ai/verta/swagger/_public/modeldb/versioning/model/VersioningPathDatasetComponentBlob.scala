// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningPathDatasetComponentBlob (
  path: Option[String] = None,
  size: Option[String] = None,
  last_modified_at_source: Option[String] = None,
  sha256: Option[String] = None,
  md5: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPathDatasetComponentBlob.toJson(this)
}

object VersioningPathDatasetComponentBlob {
  def toJson(obj: VersioningPathDatasetComponentBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.path.map(x => JField("path", JString(x))),
        obj.size.map(x => JField("size", JString(x))),
        obj.last_modified_at_source.map(x => JField("last_modified_at_source", JString(x))),
        obj.sha256.map(x => JField("sha256", JString(x))),
        obj.md5.map(x => JField("md5", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningPathDatasetComponentBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningPathDatasetComponentBlob(
          // TODO: handle required
          path = fieldsMap.get("path").map(JsonConverter.fromJsonString),
          size = fieldsMap.get("size").map(JsonConverter.fromJsonString),
          last_modified_at_source = fieldsMap.get("last_modified_at_source").map(JsonConverter.fromJsonString),
          sha256 = fieldsMap.get("sha256").map(JsonConverter.fromJsonString),
          md5 = fieldsMap.get("md5").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
