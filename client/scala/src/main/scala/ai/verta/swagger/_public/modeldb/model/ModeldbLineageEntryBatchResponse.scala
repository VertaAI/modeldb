// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class ModeldbLineageEntryBatchResponse (
  items: Option[List[ModeldbLineageEntryBatchResponseSingle]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLineageEntryBatchResponse.toJson(this)
}

object ModeldbLineageEntryBatchResponse {
  def toJson(obj: ModeldbLineageEntryBatchResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.items.map(x => JField("items", ((x: List[ModeldbLineageEntryBatchResponseSingle]) => JArray(x.map(((x: ModeldbLineageEntryBatchResponseSingle) => ModeldbLineageEntryBatchResponseSingle.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLineageEntryBatchResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLineageEntryBatchResponse(
          // TODO: handle required
          items = fieldsMap.get("items").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbLineageEntryBatchResponseSingle.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
