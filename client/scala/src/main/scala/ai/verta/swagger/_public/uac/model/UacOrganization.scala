// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.uac.model.TernaryEnumTernary._
import ai.verta.swagger.client.objects._

case class UacOrganization (
  created_timestamp: Option[BigInt] = None,
  default_dataset_collaborator_type: Option[CollaboratorTypeEnumCollaboratorType] = None,
  default_endpoint_collaborator_type: Option[CollaboratorTypeEnumCollaboratorType] = None,
  default_registered_model_collaborator_type: Option[CollaboratorTypeEnumCollaboratorType] = None,
  default_repo_collaborator_type: Option[CollaboratorTypeEnumCollaboratorType] = None,
  description: Option[String] = None,
  global_can_deploy: Option[TernaryEnumTernary] = None,
  global_collaborator_type: Option[CollaboratorTypeEnumCollaboratorType] = None,
  id: Option[String] = None,
  name: Option[String] = None,
  owner_id: Option[String] = None,
  registered_model_can_deploy: Option[TernaryEnumTernary] = None,
  short_name: Option[String] = None,
  updated_timestamp: Option[BigInt] = None,
  workspace_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = UacOrganization.toJson(this)
}

object UacOrganization {
  def toJson(obj: UacOrganization): JObject = {
    new JObject(
      List[Option[JField]](
        obj.created_timestamp.map(x => JField("created_timestamp", JInt(x))),
        obj.default_dataset_collaborator_type.map(x => JField("default_dataset_collaborator_type", ((x: CollaboratorTypeEnumCollaboratorType) => CollaboratorTypeEnumCollaboratorType.toJson(x))(x))),
        obj.default_endpoint_collaborator_type.map(x => JField("default_endpoint_collaborator_type", ((x: CollaboratorTypeEnumCollaboratorType) => CollaboratorTypeEnumCollaboratorType.toJson(x))(x))),
        obj.default_registered_model_collaborator_type.map(x => JField("default_registered_model_collaborator_type", ((x: CollaboratorTypeEnumCollaboratorType) => CollaboratorTypeEnumCollaboratorType.toJson(x))(x))),
        obj.default_repo_collaborator_type.map(x => JField("default_repo_collaborator_type", ((x: CollaboratorTypeEnumCollaboratorType) => CollaboratorTypeEnumCollaboratorType.toJson(x))(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.global_can_deploy.map(x => JField("global_can_deploy", ((x: TernaryEnumTernary) => TernaryEnumTernary.toJson(x))(x))),
        obj.global_collaborator_type.map(x => JField("global_collaborator_type", ((x: CollaboratorTypeEnumCollaboratorType) => CollaboratorTypeEnumCollaboratorType.toJson(x))(x))),
        obj.id.map(x => JField("id", JString(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.owner_id.map(x => JField("owner_id", JString(x))),
        obj.registered_model_can_deploy.map(x => JField("registered_model_can_deploy", ((x: TernaryEnumTernary) => TernaryEnumTernary.toJson(x))(x))),
        obj.short_name.map(x => JField("short_name", JString(x))),
        obj.updated_timestamp.map(x => JField("updated_timestamp", JInt(x))),
        obj.workspace_id.map(x => JField("workspace_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacOrganization =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacOrganization(
          // TODO: handle required
          created_timestamp = fieldsMap.get("created_timestamp").map(JsonConverter.fromJsonInteger),
          default_dataset_collaborator_type = fieldsMap.get("default_dataset_collaborator_type").map(CollaboratorTypeEnumCollaboratorType.fromJson),
          default_endpoint_collaborator_type = fieldsMap.get("default_endpoint_collaborator_type").map(CollaboratorTypeEnumCollaboratorType.fromJson),
          default_registered_model_collaborator_type = fieldsMap.get("default_registered_model_collaborator_type").map(CollaboratorTypeEnumCollaboratorType.fromJson),
          default_repo_collaborator_type = fieldsMap.get("default_repo_collaborator_type").map(CollaboratorTypeEnumCollaboratorType.fromJson),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          global_can_deploy = fieldsMap.get("global_can_deploy").map(TernaryEnumTernary.fromJson),
          global_collaborator_type = fieldsMap.get("global_collaborator_type").map(CollaboratorTypeEnumCollaboratorType.fromJson),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          owner_id = fieldsMap.get("owner_id").map(JsonConverter.fromJsonString),
          registered_model_can_deploy = fieldsMap.get("registered_model_can_deploy").map(TernaryEnumTernary.fromJson),
          short_name = fieldsMap.get("short_name").map(JsonConverter.fromJsonString),
          updated_timestamp = fieldsMap.get("updated_timestamp").map(JsonConverter.fromJsonInteger),
          workspace_id = fieldsMap.get("workspace_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
