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

case class ModeldbDeleteMetrics (
  delete_all: Option[Boolean] = None,
  id: Option[String] = None,
  metric_keys: Option[List[String]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbDeleteMetrics.toJson(this)
}

object ModeldbDeleteMetrics {
  def toJson(obj: ModeldbDeleteMetrics): JObject = {
    new JObject(
      List[Option[JField]](
        obj.delete_all.map(x => JField("delete_all", JBool(x))),
        obj.id.map(x => JField("id", JString(x))),
        obj.metric_keys.map(x => JField("metric_keys", ((x: List[String]) => JArray(x.map(JString)))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbDeleteMetrics =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbDeleteMetrics(
          // TODO: handle required
          delete_all = fieldsMap.get("delete_all").map(JsonConverter.fromJsonBoolean),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          metric_keys = fieldsMap.get("metric_keys").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
