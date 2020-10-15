// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.metadata.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.metadata.model.IDTypeEnumIDType._
import ai.verta.swagger._public.modeldb.metadata.model.OperatorEnumOperator._
import ai.verta.swagger.client.objects._

case class MetadataIdentificationType (
  id_type: Option[IDTypeEnumIDType] = None,
  int_id: Option[BigInt] = None,
  string_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = MetadataIdentificationType.toJson(this)
}

object MetadataIdentificationType {
  def toJson(obj: MetadataIdentificationType): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id_type.map(x => JField("id_type", ((x: IDTypeEnumIDType) => IDTypeEnumIDType.toJson(x))(x))),
        obj.int_id.map(x => JField("int_id", JInt(x))),
        obj.string_id.map(x => JField("string_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): MetadataIdentificationType =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        MetadataIdentificationType(
          // TODO: handle required
          id_type = fieldsMap.get("id_type").map(IDTypeEnumIDType.fromJson),
          int_id = fieldsMap.get("int_id").map(JsonConverter.fromJsonInteger),
          string_id = fieldsMap.get("string_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
