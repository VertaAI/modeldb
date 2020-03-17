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

case class ModeldbVersioningEntry (
  repository_id: Option[String] = None,
  commit: Option[String] = None,
  key_location_map: Option[Map[String,VertamodeldbLocation]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbVersioningEntry.toJson(this)
}

object ModeldbVersioningEntry {
  def toJson(obj: ModeldbVersioningEntry): JObject = {
    new JObject(
      List[Option[JField]](
        obj.repository_id.map(x => JField("repository_id", JString(x))),
        obj.commit.map(x => JField("commit", JString(x))),
        obj.key_location_map.map(x => JField("key_location_map", ((x: Map[String,VertamodeldbLocation]) => JObject(x.toList.map(kv => JField(kv._1,((x: VertamodeldbLocation) => VertamodeldbLocation.toJson(x))(kv._2)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbVersioningEntry =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbVersioningEntry(
          // TODO: handle required
          repository_id = fieldsMap.get("repository_id").map(JsonConverter.fromJsonString),
          commit = fieldsMap.get("commit").map(JsonConverter.fromJsonString),
          key_location_map = fieldsMap.get("key_location_map").map((x: JValue) => x match {case JObject(fields) => fields.map(kv => (kv.name, VertamodeldbLocation.fromJson(kv.value))).toMap; case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
