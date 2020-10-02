// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.modeldb.model.DatasetTypeEnumDatasetType._
import ai.verta.swagger._public.modeldb.model.DatasetVisibilityEnumDatasetVisibility._
import ai.verta.swagger._public.modeldb.model.EntitiesEnumEntitiesTypes._
import ai.verta.swagger._public.modeldb.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.modeldb.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.modeldb.model.ModeldbProjectVisibility._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.PathLocationTypeEnumPathLocationType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.ServiceEnumService._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.UacFlagEnum._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningPathDatasetComponentBlob (
  base_path: Option[String] = None,
  internal_versioned_path: Option[String] = None,
  last_modified_at_source: Option[BigInt] = None,
  md5: Option[String] = None,
  path: Option[String] = None,
  sha256: Option[String] = None,
  size: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPathDatasetComponentBlob.toJson(this)
}

object VersioningPathDatasetComponentBlob {
  def toJson(obj: VersioningPathDatasetComponentBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.base_path.map(x => JField("base_path", JString(x))),
        obj.internal_versioned_path.map(x => JField("internal_versioned_path", JString(x))),
        obj.last_modified_at_source.map(x => JField("last_modified_at_source", JInt(x))),
        obj.md5.map(x => JField("md5", JString(x))),
        obj.path.map(x => JField("path", JString(x))),
        obj.sha256.map(x => JField("sha256", JString(x))),
        obj.size.map(x => JField("size", JInt(x)))
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
          base_path = fieldsMap.get("base_path").map(JsonConverter.fromJsonString),
          internal_versioned_path = fieldsMap.get("internal_versioned_path").map(JsonConverter.fromJsonString),
          last_modified_at_source = fieldsMap.get("last_modified_at_source").map(JsonConverter.fromJsonInteger),
          md5 = fieldsMap.get("md5").map(JsonConverter.fromJsonString),
          path = fieldsMap.get("path").map(JsonConverter.fromJsonString),
          sha256 = fieldsMap.get("sha256").map(JsonConverter.fromJsonString),
          size = fieldsMap.get("size").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
