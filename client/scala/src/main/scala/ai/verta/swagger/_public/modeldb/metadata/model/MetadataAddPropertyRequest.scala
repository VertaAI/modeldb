// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.metadata.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.metadata.model.IDTypeEnumIDType._
import ai.verta.swagger._public.modeldb.metadata.model.OperatorEnumOperator._
import ai.verta.swagger.client.objects._

case class MetadataAddPropertyRequest (
  id: Option[MetadataIdentificationType] = None,
  key: Option[String] = None,
  value: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = MetadataAddPropertyRequest.toJson(this)
}

object MetadataAddPropertyRequest {
  def toJson(obj: MetadataAddPropertyRequest): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", ((x: MetadataIdentificationType) => MetadataIdentificationType.toJson(x))(x))),
        obj.key.map(x => JField("key", JString(x))),
        obj.value.map(x => JField("value", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): MetadataAddPropertyRequest =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        MetadataAddPropertyRequest(
          // TODO: handle required
          id = fieldsMap.get("id").map(MetadataIdentificationType.fromJson),
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString),
          value = fieldsMap.get("value").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
