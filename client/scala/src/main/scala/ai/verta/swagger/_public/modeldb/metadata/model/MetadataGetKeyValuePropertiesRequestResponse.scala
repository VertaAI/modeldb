// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.metadata.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.metadata.model.IDTypeEnumIDType._
import ai.verta.swagger._public.modeldb.metadata.model.OperatorEnumOperator._
import ai.verta.swagger.client.objects._

case class MetadataGetKeyValuePropertiesRequestResponse (
  key_value_property: Option[List[MetadataKeyValueStringProperty]] = None
) extends BaseSwagger {
  def toJson(): JValue = MetadataGetKeyValuePropertiesRequestResponse.toJson(this)
}

object MetadataGetKeyValuePropertiesRequestResponse {
  def toJson(obj: MetadataGetKeyValuePropertiesRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.key_value_property.map(x => JField("key_value_property", ((x: List[MetadataKeyValueStringProperty]) => JArray(x.map(((x: MetadataKeyValueStringProperty) => MetadataKeyValueStringProperty.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): MetadataGetKeyValuePropertiesRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        MetadataGetKeyValuePropertiesRequestResponse(
          // TODO: handle required
          key_value_property = fieldsMap.get("key_value_property").map((x: JValue) => x match {case JArray(elements) => elements.map(MetadataKeyValueStringProperty.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
