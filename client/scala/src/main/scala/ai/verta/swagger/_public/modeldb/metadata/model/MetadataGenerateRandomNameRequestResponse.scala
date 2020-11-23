// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.metadata.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.metadata.model.IDTypeEnumIDType._
import ai.verta.swagger._public.modeldb.metadata.model.OperatorEnumOperator._
import ai.verta.swagger.client.objects._

case class MetadataGenerateRandomNameRequestResponse (
  name: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = MetadataGenerateRandomNameRequestResponse.toJson(this)
}

object MetadataGenerateRandomNameRequestResponse {
  def toJson(obj: MetadataGenerateRandomNameRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.name.map(x => JField("name", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): MetadataGenerateRandomNameRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        MetadataGenerateRandomNameRequestResponse(
          // TODO: handle required
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
