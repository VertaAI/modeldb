// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.metadata.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.metadata.model.IDTypeEnumIDType._
import ai.verta.swagger._public.modeldb.metadata.model.OperatorEnumOperator._
import ai.verta.swagger.client.objects._

case class MetadataAddKeyValuePropertiesRequest (
  id: Option[MetadataIdentificationType] = None,
  key_value_property: Option[List[MetadataKeyValueStringProperty]] = None,
  property_name: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = MetadataAddKeyValuePropertiesRequest.toJson(this)
}

object MetadataAddKeyValuePropertiesRequest {
  def toJson(obj: MetadataAddKeyValuePropertiesRequest): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", ((x: MetadataIdentificationType) => MetadataIdentificationType.toJson(x))(x))),
        obj.key_value_property.map(x => JField("key_value_property", ((x: List[MetadataKeyValueStringProperty]) => JArray(x.map(((x: MetadataKeyValueStringProperty) => MetadataKeyValueStringProperty.toJson(x)))))(x))),
        obj.property_name.map(x => JField("property_name", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): MetadataAddKeyValuePropertiesRequest =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        MetadataAddKeyValuePropertiesRequest(
          // TODO: handle required
          id = fieldsMap.get("id").map(MetadataIdentificationType.fromJson),
          key_value_property = fieldsMap.get("key_value_property").map((x: JValue) => x match {case JArray(elements) => elements.map(MetadataKeyValueStringProperty.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          property_name = fieldsMap.get("property_name").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
