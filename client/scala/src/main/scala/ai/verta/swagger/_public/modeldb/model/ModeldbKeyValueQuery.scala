// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.model.ModeldbProjectVisibility._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbKeyValueQuery (
  key: Option[String] = None,
  value: Option[GenericObject] = None,
  value_type: Option[ValueTypeEnumValueType] = None,
  operator: Option[OperatorEnumOperator] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbKeyValueQuery.toJson(this)
}

object ModeldbKeyValueQuery {
  def toJson(obj: ModeldbKeyValueQuery): JObject = {
    new JObject(
      List[Option[JField]](
        obj.key.map(x => JField("key", JString(x))),
        obj.value.map(x => JField("value", ((x: GenericObject) => x.toJson())(x))),
        obj.value_type.map(x => JField("value_type", ((x: ValueTypeEnumValueType) => ValueTypeEnumValueType.toJson(x))(x))),
        obj.operator.map(x => JField("operator", ((x: OperatorEnumOperator) => OperatorEnumOperator.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbKeyValueQuery =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbKeyValueQuery(
          // TODO: handle required
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString),
          value = fieldsMap.get("value").map(GenericObject.fromJson),
          value_type = fieldsMap.get("value_type").map(ValueTypeEnumValueType.fromJson),
          operator = fieldsMap.get("operator").map(OperatorEnumOperator.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
