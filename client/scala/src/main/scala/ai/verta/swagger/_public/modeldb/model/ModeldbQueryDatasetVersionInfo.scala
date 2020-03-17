// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.AuthzActionEnumAuthzServiceActions._
import ai.verta.swagger._public.modeldb.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.modeldb.model.DatasetTypeEnumDatasetType._
import ai.verta.swagger._public.modeldb.model.DatasetVisibilityEnumDatasetVisibility._
import ai.verta.swagger._public.modeldb.model.EntitiesEnumEntitiesTypes._
import ai.verta.swagger._public.modeldb.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.modeldb.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.PathLocationTypeEnumPathLocationType._
import ai.verta.swagger._public.modeldb.model.RoleActionEnumRoleServiceActions._
import ai.verta.swagger._public.modeldb.model.ServiceEnumService._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.model.ModeldbProjectVisibility._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class ModeldbQueryDatasetVersionInfo (
  query: Option[String] = None,
  query_template: Option[String] = None,
  query_parameters: Option[List[ModeldbQueryParameter]] = None,
  data_source_uri: Option[String] = None,
  execution_timestamp: Option[String] = None,
  num_records: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbQueryDatasetVersionInfo.toJson(this)
}

object ModeldbQueryDatasetVersionInfo {
  def toJson(obj: ModeldbQueryDatasetVersionInfo): JObject = {
    new JObject(
      List[Option[JField]](
        obj.query.map(x => JField("query", JString(x))),
        obj.query_template.map(x => JField("query_template", JString(x))),
        obj.query_parameters.map(x => JField("query_parameters", ((x: List[ModeldbQueryParameter]) => JArray(x.map(((x: ModeldbQueryParameter) => ModeldbQueryParameter.toJson(x)))))(x))),
        obj.data_source_uri.map(x => JField("data_source_uri", JString(x))),
        obj.execution_timestamp.map(x => JField("execution_timestamp", JString(x))),
        obj.num_records.map(x => JField("num_records", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbQueryDatasetVersionInfo =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbQueryDatasetVersionInfo(
          // TODO: handle required
          query = fieldsMap.get("query").map(JsonConverter.fromJsonString),
          query_template = fieldsMap.get("query_template").map(JsonConverter.fromJsonString),
          query_parameters = fieldsMap.get("query_parameters").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbQueryParameter.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          data_source_uri = fieldsMap.get("data_source_uri").map(JsonConverter.fromJsonString),
          execution_timestamp = fieldsMap.get("execution_timestamp").map(JsonConverter.fromJsonString),
          num_records = fieldsMap.get("num_records").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
