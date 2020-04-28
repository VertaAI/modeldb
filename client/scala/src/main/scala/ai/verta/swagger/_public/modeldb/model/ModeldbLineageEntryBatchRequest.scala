// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class ModeldbLineageEntryBatchRequest (
  entry: Option[ModeldbLineageEntry] = None,
  id: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLineageEntryBatchRequest.toJson(this)
}

object ModeldbLineageEntryBatchRequest {
  def toJson(obj: ModeldbLineageEntryBatchRequest): JObject = {
    new JObject(
      List[Option[JField]](
        obj.entry.map(x => JField("entry", ((x: ModeldbLineageEntry) => ModeldbLineageEntry.toJson(x))(x))),
        obj.id.map(x => JField("id", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLineageEntryBatchRequest =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLineageEntryBatchRequest(
          // TODO: handle required
          entry = fieldsMap.get("entry").map(ModeldbLineageEntry.fromJson),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
