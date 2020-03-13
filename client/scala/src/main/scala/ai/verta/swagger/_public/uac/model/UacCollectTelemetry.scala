// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.uac.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class UacCollectTelemetry (
  id: Option[String] = None,
  metrics: Option[List[CommonKeyValue]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacCollectTelemetry.toJson(this)
}

object UacCollectTelemetry {
  def toJson(obj: UacCollectTelemetry): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.metrics.map(x => JField("metrics", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacCollectTelemetry =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacCollectTelemetry(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          metrics = fieldsMap.get("metrics").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
