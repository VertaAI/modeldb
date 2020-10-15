// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.metadata.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.metadata.model.IDTypeEnumIDType._
import ai.verta.swagger._public.modeldb.metadata.model.OperatorEnumOperator._
import ai.verta.swagger.client.objects._

case class MetadataGetLabelsRequestResponse (
  labels: Option[List[String]] = None
) extends BaseSwagger {
  def toJson(): JValue = MetadataGetLabelsRequestResponse.toJson(this)
}

object MetadataGetLabelsRequestResponse {
  def toJson(obj: MetadataGetLabelsRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.labels.map(x => JField("labels", ((x: List[String]) => JArray(x.map(JString)))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): MetadataGetLabelsRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        MetadataGetLabelsRequestResponse(
          // TODO: handle required
          labels = fieldsMap.get("labels").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
