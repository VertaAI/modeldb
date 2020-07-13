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

case class VersioningS3DatasetComponentBlob (
  path: Option[VersioningPathDatasetComponentBlob] = None,
  s3_version_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningS3DatasetComponentBlob.toJson(this)
}

object VersioningS3DatasetComponentBlob {
  def toJson(obj: VersioningS3DatasetComponentBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.path.map(x => JField("path", ((x: VersioningPathDatasetComponentBlob) => VersioningPathDatasetComponentBlob.toJson(x))(x))),
        obj.s3_version_id.map(x => JField("s3_version_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningS3DatasetComponentBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningS3DatasetComponentBlob(
          // TODO: handle required
          path = fieldsMap.get("path").map(VersioningPathDatasetComponentBlob.fromJson),
          s3_version_id = fieldsMap.get("s3_version_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
