// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbLogMetric (
  id: Option[String] = None,
  metric: Option[CommonKeyValue] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLogMetric.toJson(this)
}

object ModeldbLogMetric {
  def toJson(obj: ModeldbLogMetric): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.metric.map(x => JField("metric", ((x: CommonKeyValue) => CommonKeyValue.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLogMetric =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLogMetric(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          metric = fieldsMap.get("metric").map(CommonKeyValue.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
