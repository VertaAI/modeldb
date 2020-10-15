// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.registry.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.registry.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.registry.model.OperatorEnumOperator._
import ai.verta.swagger._public.registry.model.ProtobufNullValue._
import ai.verta.swagger._public.registry.model.TernaryEnumTernary._
import ai.verta.swagger._public.registry.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.registry.model.VisibilityEnumVisibility._
import ai.verta.swagger.client.objects._

case class CommonPagination (
  page_limit: Option[BigInt] = None,
  page_number: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = CommonPagination.toJson(this)
}

object CommonPagination {
  def toJson(obj: CommonPagination): JObject = {
    new JObject(
      List[Option[JField]](
        obj.page_limit.map(x => JField("page_limit", JInt(x))),
        obj.page_number.map(x => JField("page_number", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): CommonPagination =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        CommonPagination(
          // TODO: handle required
          page_limit = fieldsMap.get("page_limit").map(JsonConverter.fromJsonInteger),
          page_number = fieldsMap.get("page_number").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
