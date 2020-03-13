// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.LineageEntryEnumLineageEntryType._
import ai.verta.swagger.client.objects._

case class ModeldbLineageEntry (
  `type`: Option[LineageEntryEnumLineageEntryType] = None,
  external_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLineageEntry.toJson(this)
}

object ModeldbLineageEntry {
  def toJson(obj: ModeldbLineageEntry): JObject = {
    new JObject(
      List[Option[JField]](
        obj.`type`.map(x => JField("`type`", ((x: LineageEntryEnumLineageEntryType) => LineageEntryEnumLineageEntryType.toJson(x))(x))),
        obj.external_id.map(x => JField("external_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLineageEntry =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLineageEntry(
          // TODO: handle required
          `type` = fieldsMap.get("`type`").map(LineageEntryEnumLineageEntryType.fromJson),
          external_id = fieldsMap.get("external_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
