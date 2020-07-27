// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.metadata.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.metadata.model.IDTypeEnumIDType._
import ai.verta.swagger._public.modeldb.metadata.model.OperatorEnumOperator._
import ai.verta.swagger.client.objects._

case class MetadataAddLabelsRequest (
  id: Option[MetadataIdentificationType] = None,
  labels: Option[List[String]] = None
) extends BaseSwagger {
  def toJson(): JValue = MetadataAddLabelsRequest.toJson(this)
}

object MetadataAddLabelsRequest {
  def toJson(obj: MetadataAddLabelsRequest): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", ((x: MetadataIdentificationType) => MetadataIdentificationType.toJson(x))(x))),
        obj.labels.map(x => JField("labels", ((x: List[String]) => JArray(x.map(JString)))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): MetadataAddLabelsRequest =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        MetadataAddLabelsRequest(
          // TODO: handle required
          id = fieldsMap.get("id").map(MetadataIdentificationType.fromJson),
          labels = fieldsMap.get("labels").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
