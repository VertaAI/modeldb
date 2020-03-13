// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.LineageEntryEnumLineageEntryType._
import ai.verta.swagger.client.objects._

case class ModeldbFindAllInputsOutputsResponse (
  inputs: Option[List[ModeldbLineageEntryBatch]] = None,
  outputs: Option[List[ModeldbLineageEntryBatch]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbFindAllInputsOutputsResponse.toJson(this)
}

object ModeldbFindAllInputsOutputsResponse {
  def toJson(obj: ModeldbFindAllInputsOutputsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.inputs.map(x => JField("inputs", ((x: List[ModeldbLineageEntryBatch]) => JArray(x.map(((x: ModeldbLineageEntryBatch) => ModeldbLineageEntryBatch.toJson(x)))))(x))),
        obj.outputs.map(x => JField("outputs", ((x: List[ModeldbLineageEntryBatch]) => JArray(x.map(((x: ModeldbLineageEntryBatch) => ModeldbLineageEntryBatch.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbFindAllInputsOutputsResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbFindAllInputsOutputsResponse(
          // TODO: handle required
          inputs = fieldsMap.get("inputs").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbLineageEntryBatch.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          outputs = fieldsMap.get("outputs").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbLineageEntryBatch.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
