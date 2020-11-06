// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.uac.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.uac.model.UacFlagEnum._
import ai.verta.swagger.client.objects._

case class UacTrialUserInfo (
  days_remaining: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = UacTrialUserInfo.toJson(this)
}

object UacTrialUserInfo {
  def toJson(obj: UacTrialUserInfo): JObject = {
    new JObject(
      List[Option[JField]](
        obj.days_remaining.map(x => JField("days_remaining", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): UacTrialUserInfo =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        UacTrialUserInfo(
          // TODO: handle required
          days_remaining = fieldsMap.get("days_remaining").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
