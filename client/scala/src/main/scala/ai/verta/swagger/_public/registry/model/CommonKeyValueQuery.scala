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

case class CommonKeyValueQuery (
  key: Option[String] = None,
  operator: Option[OperatorEnumOperator] = None,
  value: Option[GenericObject] = None,
  value_type: Option[ValueTypeEnumValueType] = None
) extends BaseSwagger {
  def toJson(): JValue = CommonKeyValueQuery.toJson(this)
}

object CommonKeyValueQuery {
  def toJson(obj: CommonKeyValueQuery): JObject = {
    new JObject(
      List[Option[JField]](
        obj.key.map(x => JField("key", JString(x))),
        obj.operator.map(x => JField("operator", ((x: OperatorEnumOperator) => OperatorEnumOperator.toJson(x))(x))),
        obj.value.map(x => JField("value", ((x: GenericObject) => x.toJson())(x))),
        obj.value_type.map(x => JField("value_type", ((x: ValueTypeEnumValueType) => ValueTypeEnumValueType.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): CommonKeyValueQuery =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        CommonKeyValueQuery(
          // TODO: handle required
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString),
          operator = fieldsMap.get("operator").map(OperatorEnumOperator.fromJson),
          value = fieldsMap.get("value").map(GenericObject.fromJson),
          value_type = fieldsMap.get("value_type").map(ValueTypeEnumValueType.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
