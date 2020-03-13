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

case class ModeldbGetHydratedDatasetByNameResponse (
  hydrated_dataset_by_user: Option[ModeldbHydratedDataset] = None,
  shared_hydrated_datasets: Option[List[ModeldbHydratedDataset]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetHydratedDatasetByNameResponse.toJson(this)
}

object ModeldbGetHydratedDatasetByNameResponse {
  def toJson(obj: ModeldbGetHydratedDatasetByNameResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.hydrated_dataset_by_user.map(x => JField("hydrated_dataset_by_user", ((x: ModeldbHydratedDataset) => ModeldbHydratedDataset.toJson(x))(x))),
        obj.shared_hydrated_datasets.map(x => JField("shared_hydrated_datasets", ((x: List[ModeldbHydratedDataset]) => JArray(x.map(((x: ModeldbHydratedDataset) => ModeldbHydratedDataset.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetHydratedDatasetByNameResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetHydratedDatasetByNameResponse(
          // TODO: handle required
          hydrated_dataset_by_user = fieldsMap.get("hydrated_dataset_by_user").map(ModeldbHydratedDataset.fromJson),
          shared_hydrated_datasets = fieldsMap.get("shared_hydrated_datasets").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbHydratedDataset.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
