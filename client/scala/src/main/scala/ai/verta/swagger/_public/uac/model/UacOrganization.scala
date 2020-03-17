// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.uac.model.TernaryEnumTernary._
import ai.verta.swagger.client.objects._

case class UacOrganization (
  id: Option[String] = None,
  name: Option[String] = None,
  short_name: Option[String] = None,
  description: Option[String] = None,
  owner_id: Option[String] = None,
  created_timestamp: Option[String] = None,
  updated_timestamp: Option[String] = None,
  global_collaborator_type: Option[CollaboratorTypeEnumCollaboratorType] = None,
  global_can_deploy: Option[TernaryEnumTernary] = None
) extends BaseSwagger {
  def toJson(): JValue = UacOrganization.toJson(this)
}

object UacOrganization {
  def toJson(obj: UacOrganization): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.short_name.map(x => JField("short_name", JString(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.owner_id.map(x => JField("owner_id", JString(x))),
        obj.created_timestamp.map(x => JField("created_timestamp", JString(x))),
        obj.updated_timestamp.map(x => JField("updated_timestamp", JString(x))),
        obj.global_collaborator_type.map(x => JField("global_collaborator_type", ((x: CollaboratorTypeEnumCollaboratorType) => CollaboratorTypeEnumCollaboratorType.toJson(x))(x))),
        obj.global_can_deploy.map(x => JField("global_can_deploy", ((x: TernaryEnumTernary) => TernaryEnumTernary.toJson(x))(x)))
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
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          short_name = fieldsMap.get("short_name").map(JsonConverter.fromJsonString),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          owner_id = fieldsMap.get("owner_id").map(JsonConverter.fromJsonString),
          created_timestamp = fieldsMap.get("created_timestamp").map(JsonConverter.fromJsonString),
          updated_timestamp = fieldsMap.get("updated_timestamp").map(JsonConverter.fromJsonString),
          global_collaborator_type = fieldsMap.get("global_collaborator_type").map(CollaboratorTypeEnumCollaboratorType.fromJson),
          global_can_deploy = fieldsMap.get("global_can_deploy").map(TernaryEnumTernary.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
