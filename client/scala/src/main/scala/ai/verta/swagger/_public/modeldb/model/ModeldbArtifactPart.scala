// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger.client.objects._

case class ModeldbArtifactPart (
  etag: Option[String] = None,
  part_number: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbArtifactPart.toJson(this)
}

object ModeldbArtifactPart {
  def toJson(obj: ModeldbArtifactPart): JObject = {
    new JObject(
      List[Option[JField]](
        obj.etag.map(x => JField("etag", JString(x))),
        obj.part_number.map(x => JField("part_number", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbArtifactPart =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbArtifactPart(
          // TODO: handle required
          etag = fieldsMap.get("etag").map(JsonConverter.fromJsonString),
          part_number = fieldsMap.get("part_number").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
