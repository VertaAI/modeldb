// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class VersioningRangeTimeStampPredicate (
  from_ts: Option[BigInt] = None,
  to_ts: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningRangeTimeStampPredicate.toJson(this)
}

object VersioningRangeTimeStampPredicate {
  def toJson(obj: VersioningRangeTimeStampPredicate): JObject = {
    new JObject(
      List[Option[JField]](
        obj.from_ts.map(x => JField("from_ts", JInt(x))),
        obj.to_ts.map(x => JField("to_ts", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningRangeTimeStampPredicate =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningRangeTimeStampPredicate(
          // TODO: handle required
          from_ts = fieldsMap.get("from_ts").map(JsonConverter.fromJsonInteger),
          to_ts = fieldsMap.get("to_ts").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
