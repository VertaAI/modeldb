// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class ModeldbFindAllOutputsResponse (
  outputs: Option[List[ModeldbLineageEntryBatchResponse]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbFindAllOutputsResponse.toJson(this)
}

object ModeldbFindAllOutputsResponse {
  def toJson(obj: ModeldbFindAllOutputsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.outputs.map(x => JField("outputs", ((x: List[ModeldbLineageEntryBatchResponse]) => JArray(x.map(((x: ModeldbLineageEntryBatchResponse) => ModeldbLineageEntryBatchResponse.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbFindAllOutputsResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbFindAllOutputsResponse(
          // TODO: handle required
          outputs = fieldsMap.get("outputs").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbLineageEntryBatchResponse.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
