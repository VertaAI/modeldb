// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.uac.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class UacGetUsersFuzzyResponse (
  total_records: Option[BigInt] = None,
  user_infos: Option[List[UacUserInfo]] = None
) extends BaseSwagger {
  def toJson(): JValue = UacGetUsersFuzzyResponse.toJson(this)
}

object UacGetUsersFuzzyResponse {
  def toJson(obj: UacGetUsersFuzzyResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.total_records.map(x => JField("total_records", JInt(x))),
        obj.user_infos.map(x => JField("user_infos", ((x: List[UacUserInfo]) => JArray(x.map(((x: UacUserInfo) => UacUserInfo.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacGetUsersFuzzyResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacGetUsersFuzzyResponse(
          // TODO: handle required
          total_records = fieldsMap.get("total_records").map(JsonConverter.fromJsonInteger),
          user_infos = fieldsMap.get("user_infos").map((x: JValue) => x match {case JArray(elements) => elements.map(UacUserInfo.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
