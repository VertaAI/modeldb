// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class VersioningBatchResponseRow (
  acknowledge: Option[Boolean] = None,
  error: Option[String] = None,
  error_code: Option[BigInt] = None,
  local_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningBatchResponseRow.toJson(this)
}

object VersioningBatchResponseRow {
  def toJson(obj: VersioningBatchResponseRow): JObject = {
    new JObject(
      List[Option[JField]](
        obj.acknowledge.map(x => JField("acknowledge", JBool(x))),
        obj.error.map(x => JField("error", JString(x))),
        obj.error_code.map(x => JField("error_code", JInt(x))),
        obj.local_id.map(x => JField("local_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningBatchResponseRow =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningBatchResponseRow(
          // TODO: handle required
          acknowledge = fieldsMap.get("acknowledge").map(JsonConverter.fromJsonBoolean),
          error = fieldsMap.get("error").map(JsonConverter.fromJsonString),
          error_code = fieldsMap.get("error_code").map(JsonConverter.fromJsonInteger),
          local_id = fieldsMap.get("local_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
