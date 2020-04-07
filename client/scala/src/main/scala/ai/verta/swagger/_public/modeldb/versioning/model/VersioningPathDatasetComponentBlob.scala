// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.RepositoryVisibilityEnumRepositoryVisibility._
import ai.verta.swagger._public.modeldb.versioning.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.versioning.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.versioning.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class VersioningPathDatasetComponentBlob (
  last_modified_at_source: Option[] = None,
  md5: Option[String] = None,
  path: Option[String] = None,
  sha256: Option[String] = None,
  size: Option[] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPathDatasetComponentBlob.toJson(this)
}

object VersioningPathDatasetComponentBlob {
  def toJson(obj: VersioningPathDatasetComponentBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.last_modified_at_source.map(x => JField("last_modified_at_source", (x))),
        obj.md5.map(x => JField("md5", JString(x))),
        obj.path.map(x => JField("path", JString(x))),
        obj.sha256.map(x => JField("sha256", JString(x))),
        obj.size.map(x => JField("size", (x)))
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
          last_modified_at_source = fieldsMap.get("last_modified_at_source").map(),
          md5 = fieldsMap.get("md5").map(JsonConverter.fromJsonString),
          path = fieldsMap.get("path").map(JsonConverter.fromJsonString),
          sha256 = fieldsMap.get("sha256").map(JsonConverter.fromJsonString),
          size = fieldsMap.get("size").map()
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
