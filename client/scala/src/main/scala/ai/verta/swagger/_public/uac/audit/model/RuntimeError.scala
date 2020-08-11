// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class RuntimeError (
  code: Option[BigInt] = None,
  details: Option[List[ProtobufAny]] = None,
  error: Option[String] = None,
  message: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = RuntimeError.toJson(this)
}

object RuntimeError {
  def toJson(obj: RuntimeError): JObject = {
    new JObject(
      List[Option[JField]](
        obj.code.map(x => JField("code", JInt(x))),
        obj.details.map(x => JField("details", ((x: List[ProtobufAny]) => JArray(x.map(((x: ProtobufAny) => ProtobufAny.toJson(x)))))(x))),
        obj.error.map(x => JField("error", JString(x))),
        obj.message.map(x => JField("message", JString(x)))
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
          code = fieldsMap.get("code").map(JsonConverter.fromJsonInteger),
          details = fieldsMap.get("details").map((x: JValue) => x match {case JArray(elements) => elements.map(ProtobufAny.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          error = fieldsMap.get("error").map(JsonConverter.fromJsonString),
          message = fieldsMap.get("message").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
