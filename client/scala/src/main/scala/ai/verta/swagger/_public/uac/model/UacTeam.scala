// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class UacTeam (
  created_timestamp: Option[BigInt] = None,
  description: Option[String] = None,
  id: Option[String] = None,
  name: Option[String] = None,
  org_id: Option[String] = None,
  owner_id: Option[String] = None,
  short_name: Option[String] = None,
  updated_timestamp: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = UacTeam.toJson(this)
}

object UacTeam {
  def toJson(obj: UacTeam): JObject = {
    new JObject(
      List[Option[JField]](
        obj.created_timestamp.map(x => JField("created_timestamp", JInt(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.id.map(x => JField("id", JString(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.org_id.map(x => JField("org_id", JString(x))),
        obj.owner_id.map(x => JField("owner_id", JString(x))),
        obj.short_name.map(x => JField("short_name", JString(x))),
        obj.updated_timestamp.map(x => JField("updated_timestamp", JInt(x)))
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
          created_timestamp = fieldsMap.get("created_timestamp").map(JsonConverter.fromJsonInteger),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          org_id = fieldsMap.get("org_id").map(JsonConverter.fromJsonString),
          owner_id = fieldsMap.get("owner_id").map(JsonConverter.fromJsonString),
          short_name = fieldsMap.get("short_name").map(JsonConverter.fromJsonString),
          updated_timestamp = fieldsMap.get("updated_timestamp").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
