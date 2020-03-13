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

case class ModeldbMetricsSummary (
  key: Option[String] = None,
  min_value: Option[Double] = None,
  max_value: Option[Double] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbMetricsSummary.toJson(this)
}

object ModeldbMetricsSummary {
  def toJson(obj: ModeldbMetricsSummary): JObject = {
    new JObject(
      List[Option[JField]](
        obj.key.map(x => JField("key", JString(x))),
        obj.min_value.map(x => JField("min_value", JDouble(x))),
        obj.max_value.map(x => JField("max_value", JDouble(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbMetricsSummary =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbMetricsSummary(
          // TODO: handle required
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString),
          min_value = fieldsMap.get("min_value").map(JsonConverter.fromJsonDouble),
          max_value = fieldsMap.get("max_value").map(JsonConverter.fromJsonDouble)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
