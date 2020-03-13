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

case class ModeldbPathDatasetVersionInfo (
  location_type: Option[PathLocationTypeEnumPathLocationType] = None,
  size: Option[String] = None,
  dataset_part_infos: Option[List[ModeldbDatasetPartInfo]] = None,
  base_path: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbPathDatasetVersionInfo.toJson(this)
}

object ModeldbPathDatasetVersionInfo {
  def toJson(obj: ModeldbPathDatasetVersionInfo): JObject = {
    new JObject(
      List[Option[JField]](
        obj.location_type.map(x => JField("location_type", ((x: PathLocationTypeEnumPathLocationType) => PathLocationTypeEnumPathLocationType.toJson(x))(x))),
        obj.size.map(x => JField("size", JString(x))),
        obj.dataset_part_infos.map(x => JField("dataset_part_infos", ((x: List[ModeldbDatasetPartInfo]) => JArray(x.map(((x: ModeldbDatasetPartInfo) => ModeldbDatasetPartInfo.toJson(x)))))(x))),
        obj.base_path.map(x => JField("base_path", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbPathDatasetVersionInfo =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbPathDatasetVersionInfo(
          // TODO: handle required
          location_type = fieldsMap.get("location_type").map(PathLocationTypeEnumPathLocationType.fromJson),
          size = fieldsMap.get("size").map(JsonConverter.fromJsonString),
          dataset_part_infos = fieldsMap.get("dataset_part_infos").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbDatasetPartInfo.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          base_path = fieldsMap.get("base_path").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
