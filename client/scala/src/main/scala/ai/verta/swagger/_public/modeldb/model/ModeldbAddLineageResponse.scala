// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class ModeldbAddLineageResponse (
  id: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbAddLineageResponse.toJson(this)
}

object ModeldbAddLineageResponse {
  def toJson(obj: ModeldbAddLineageResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbAddLineageResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbAddLineageResponse(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
