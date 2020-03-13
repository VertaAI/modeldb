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

case class UacTeam (
  id: Option[String] = None,
  org_id: Option[String] = None,
  name: Option[String] = None,
  short_name: Option[String] = None,
  description: Option[String] = None,
  owner_id: Option[String] = None,
  created_timestamp: Option[String] = None,
  updated_timestamp: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = UacTeam.toJson(this)
}

object UacTeam {
  def toJson(obj: UacTeam): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.org_id.map(x => JField("org_id", JString(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.short_name.map(x => JField("short_name", JString(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.owner_id.map(x => JField("owner_id", JString(x))),
        obj.created_timestamp.map(x => JField("created_timestamp", JString(x))),
        obj.updated_timestamp.map(x => JField("updated_timestamp", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacTeam =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacTeam(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          org_id = fieldsMap.get("org_id").map(JsonConverter.fromJsonString),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          short_name = fieldsMap.get("short_name").map(JsonConverter.fromJsonString),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          owner_id = fieldsMap.get("owner_id").map(JsonConverter.fromJsonString),
          created_timestamp = fieldsMap.get("created_timestamp").map(JsonConverter.fromJsonString),
          updated_timestamp = fieldsMap.get("updated_timestamp").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
