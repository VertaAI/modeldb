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

case class ModeldbAdvancedQueryExperimentRunsResponse (
  hydrated_experiment_runs: Option[List[ModeldbHydratedExperimentRun]] = None,
  total_records: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbAdvancedQueryExperimentRunsResponse.toJson(this)
}

object ModeldbAdvancedQueryExperimentRunsResponse {
  def toJson(obj: ModeldbAdvancedQueryExperimentRunsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.hydrated_experiment_runs.map(x => JField("hydrated_experiment_runs", ((x: List[ModeldbHydratedExperimentRun]) => JArray(x.map(((x: ModeldbHydratedExperimentRun) => ModeldbHydratedExperimentRun.toJson(x)))))(x))),
        obj.total_records.map(x => JField("total_records", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbAdvancedQueryExperimentRunsResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbAdvancedQueryExperimentRunsResponse(
          // TODO: handle required
          hydrated_experiment_runs = fieldsMap.get("hydrated_experiment_runs").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbHydratedExperimentRun.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          total_records = fieldsMap.get("total_records").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
