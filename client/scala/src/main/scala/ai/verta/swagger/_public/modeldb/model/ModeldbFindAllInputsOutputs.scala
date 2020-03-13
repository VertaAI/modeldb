// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.LineageEntryEnumLineageEntryType._
import ai.verta.swagger.client.objects._

case class ModeldbFindAllInputsOutputs (
  items: Option[List[ModeldbLineageEntry]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbFindAllInputsOutputs.toJson(this)
}

object ModeldbFindAllInputsOutputs {
  def toJson(obj: ModeldbFindAllInputsOutputs): JObject = {
    new JObject(
      List[Option[JField]](
        obj.items.map(x => JField("items", ((x: List[ModeldbLineageEntry]) => JArray(x.map(((x: ModeldbLineageEntry) => ModeldbLineageEntry.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbFindAllInputsOutputs =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbFindAllInputsOutputs(
          // TODO: handle required
          items = fieldsMap.get("items").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbLineageEntry.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
