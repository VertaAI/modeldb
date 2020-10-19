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

case class ModeldbLogDatasets (
  datasets: Option[List[CommonArtifact]] = None,
  id: Option[String] = None,
  overwrite: Option[Boolean] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLogDatasets.toJson(this)
}

object ModeldbLogDatasets {
  def toJson(obj: ModeldbLogDatasets): JObject = {
    new JObject(
      List[Option[JField]](
        obj.datasets.map(x => JField("datasets", ((x: List[CommonArtifact]) => JArray(x.map(((x: CommonArtifact) => CommonArtifact.toJson(x)))))(x))),
        obj.id.map(x => JField("id", JString(x))),
        obj.overwrite.map(x => JField("overwrite", JBool(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLogDatasets =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLogDatasets(
          // TODO: handle required
          datasets = fieldsMap.get("datasets").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonArtifact.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          overwrite = fieldsMap.get("overwrite").map(JsonConverter.fromJsonBoolean)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
