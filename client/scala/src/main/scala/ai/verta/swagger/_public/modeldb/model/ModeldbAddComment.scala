// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.modeldb.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class ModeldbAddComment (
  entity_id: Option[String] = None,
  date_time: Option[String] = None,
  message: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbAddComment.toJson(this)
}

object ModeldbAddComment {
  def toJson(obj: ModeldbAddComment): JObject = {
    new JObject(
      List[Option[JField]](
        obj.entity_id.map(x => JField("entity_id", JString(x))),
        obj.date_time.map(x => JField("date_time", JString(x))),
        obj.message.map(x => JField("message", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbAddComment =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbAddComment(
          // TODO: handle required
          entity_id = fieldsMap.get("entity_id").map(JsonConverter.fromJsonString),
          date_time = fieldsMap.get("date_time").map(JsonConverter.fromJsonString),
          message = fieldsMap.get("message").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
