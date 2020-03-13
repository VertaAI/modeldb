// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.common.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class RuntimeError (
  error: Option[String] = None,
  code: Option[BigInt] = None,
  message: Option[String] = None,
  details: Option[List[ProtobufAny]] = None
) extends BaseSwagger {
  def toJson(): JValue = RuntimeError.toJson(this)
}

object RuntimeError {
  def toJson(obj: RuntimeError): JObject = {
    new JObject(
      List[Option[JField]](
        obj.error.map(x => JField("error", JString(x))),
        obj.code.map(x => JField("code", JInt(x))),
        obj.message.map(x => JField("message", JString(x))),
        obj.details.map(x => JField("details", ((x: List[ProtobufAny]) => JArray(x.map(((x: ProtobufAny) => ProtobufAny.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RuntimeError =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RuntimeError(
          // TODO: handle required
          error = fieldsMap.get("error").map(JsonConverter.fromJsonString),
          code = fieldsMap.get("code").map(JsonConverter.fromJsonInteger),
          message = fieldsMap.get("message").map(JsonConverter.fromJsonString),
          details = fieldsMap.get("details").map((x: JValue) => x match {case JArray(elements) => elements.map(ProtobufAny.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
