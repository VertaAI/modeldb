// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.LineageEntryEnumLineageEntryType._
import ai.verta.swagger.client.objects._

case class ModeldbAddLineage (
  input: Option[List[ModeldbLineageEntry]] = None,
  output: Option[List[ModeldbLineageEntry]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbAddLineage.toJson(this)
}

object ModeldbAddLineage {
  def toJson(obj: ModeldbAddLineage): JObject = {
    new JObject(
      List[Option[JField]](
        obj.input.map(x => JField("input", ((x: List[ModeldbLineageEntry]) => JArray(x.map(((x: ModeldbLineageEntry) => ModeldbLineageEntry.toJson(x)))))(x))),
        obj.output.map(x => JField("output", ((x: List[ModeldbLineageEntry]) => JArray(x.map(((x: ModeldbLineageEntry) => ModeldbLineageEntry.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbAddLineage =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbAddLineage(
          // TODO: handle required
          input = fieldsMap.get("input").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbLineageEntry.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          output = fieldsMap.get("output").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbLineageEntry.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
