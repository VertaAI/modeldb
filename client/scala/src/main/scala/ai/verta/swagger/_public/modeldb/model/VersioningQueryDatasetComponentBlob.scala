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

case class VersioningQueryDatasetComponentBlob (
  data_source_uri: Option[String] = None,
  execution_timestamp: Option[BigInt] = None,
  num_records: Option[BigInt] = None,
  query: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningQueryDatasetComponentBlob.toJson(this)
}

object VersioningQueryDatasetComponentBlob {
  def toJson(obj: VersioningQueryDatasetComponentBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.data_source_uri.map(x => JField("data_source_uri", JString(x))),
        obj.execution_timestamp.map(x => JField("execution_timestamp", JInt(x))),
        obj.num_records.map(x => JField("num_records", JInt(x))),
        obj.query.map(x => JField("query", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningQueryDatasetComponentBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningQueryDatasetComponentBlob(
          // TODO: handle required
          data_source_uri = fieldsMap.get("data_source_uri").map(JsonConverter.fromJsonString),
          execution_timestamp = fieldsMap.get("execution_timestamp").map(JsonConverter.fromJsonInteger),
          num_records = fieldsMap.get("num_records").map(JsonConverter.fromJsonInteger),
          query = fieldsMap.get("query").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
