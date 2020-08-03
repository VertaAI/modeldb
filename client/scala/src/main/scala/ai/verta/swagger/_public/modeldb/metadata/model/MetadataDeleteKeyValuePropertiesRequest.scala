// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.metadata.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.metadata.model.IDTypeEnumIDType._
import ai.verta.swagger._public.modeldb.metadata.model.OperatorEnumOperator._
import ai.verta.swagger.client.objects._

case class MetadataDeleteKeyValuePropertiesRequest (
  deleteAll: Option[Boolean] = None,
  id: Option[MetadataIdentificationType] = None,
  keys: Option[List[String]] = None,
  property_name: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = MetadataDeleteKeyValuePropertiesRequest.toJson(this)
}

object MetadataDeleteKeyValuePropertiesRequest {
  def toJson(obj: MetadataDeleteKeyValuePropertiesRequest): JObject = {
    new JObject(
      List[Option[JField]](
        obj.deleteAll.map(x => JField("deleteAll", JBool(x))),
        obj.id.map(x => JField("id", ((x: MetadataIdentificationType) => MetadataIdentificationType.toJson(x))(x))),
        obj.keys.map(x => JField("keys", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.property_name.map(x => JField("property_name", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): MetadataDeleteKeyValuePropertiesRequest =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        MetadataDeleteKeyValuePropertiesRequest(
          // TODO: handle required
          deleteAll = fieldsMap.get("deleteAll").map(JsonConverter.fromJsonBoolean),
          id = fieldsMap.get("id").map(MetadataIdentificationType.fromJson),
          keys = fieldsMap.get("keys").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          property_name = fieldsMap.get("property_name").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
