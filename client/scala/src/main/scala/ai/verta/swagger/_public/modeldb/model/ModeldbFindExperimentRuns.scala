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

case class ModeldbFindExperimentRuns (
  project_id: Option[String] = None,
  experiment_id: Option[String] = None,
  experiment_run_ids: Option[List[String]] = None,
  predicates: Option[List[ModeldbKeyValueQuery]] = None,
  ids_only: Option[Boolean] = None,
  page_number: Option[BigInt] = None,
  page_limit: Option[BigInt] = None,
  ascending: Option[Boolean] = None,
  sort_key: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbFindExperimentRuns.toJson(this)
}

object ModeldbFindExperimentRuns {
  def toJson(obj: ModeldbFindExperimentRuns): JObject = {
    new JObject(
      List[Option[JField]](
        obj.project_id.map(x => JField("project_id", JString(x))),
        obj.experiment_id.map(x => JField("experiment_id", JString(x))),
        obj.experiment_run_ids.map(x => JField("experiment_run_ids", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.predicates.map(x => JField("predicates", ((x: List[ModeldbKeyValueQuery]) => JArray(x.map(((x: ModeldbKeyValueQuery) => ModeldbKeyValueQuery.toJson(x)))))(x))),
        obj.ids_only.map(x => JField("ids_only", JBool(x))),
        obj.page_number.map(x => JField("page_number", JInt(x))),
        obj.page_limit.map(x => JField("page_limit", JInt(x))),
        obj.ascending.map(x => JField("ascending", JBool(x))),
        obj.sort_key.map(x => JField("sort_key", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbFindExperimentRuns =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbFindExperimentRuns(
          // TODO: handle required
          project_id = fieldsMap.get("project_id").map(JsonConverter.fromJsonString),
          experiment_id = fieldsMap.get("experiment_id").map(JsonConverter.fromJsonString),
          experiment_run_ids = fieldsMap.get("experiment_run_ids").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          predicates = fieldsMap.get("predicates").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbKeyValueQuery.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          ids_only = fieldsMap.get("ids_only").map(JsonConverter.fromJsonBoolean),
          page_number = fieldsMap.get("page_number").map(JsonConverter.fromJsonInteger),
          page_limit = fieldsMap.get("page_limit").map(JsonConverter.fromJsonInteger),
          ascending = fieldsMap.get("ascending").map(JsonConverter.fromJsonBoolean),
          sort_key = fieldsMap.get("sort_key").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
