// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.metadata.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.metadata.model.IDTypeEnumIDType._
import ai.verta.swagger.client.objects._

case class MetadataGetLabelIdsRequestResponse (
  ids: Option[List[MetadataIdentificationType]] = None
) extends BaseSwagger {
  def toJson(): JValue = MetadataGetLabelIdsRequestResponse.toJson(this)
}

object MetadataGetLabelIdsRequestResponse {
  def toJson(obj: MetadataGetLabelIdsRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.ids.map(x => JField("ids", ((x: List[MetadataIdentificationType]) => JArray(x.map(((x: MetadataIdentificationType) => MetadataIdentificationType.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): MetadataGetLabelIdsRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        MetadataGetLabelIdsRequestResponse(
          // TODO: handle required
          ids = fieldsMap.get("ids").map((x: JValue) => x match {case JArray(elements) => elements.map(MetadataIdentificationType.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
