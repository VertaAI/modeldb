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

case class ModeldbFindExperimentsResponse (
  experiments: Option[List[ModeldbExperiment]] = None,
  total_records: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbFindExperimentsResponse.toJson(this)
}

object ModeldbFindExperimentsResponse {
  def toJson(obj: ModeldbFindExperimentsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.experiments.map(x => JField("experiments", ((x: List[ModeldbExperiment]) => JArray(x.map(((x: ModeldbExperiment) => ModeldbExperiment.toJson(x)))))(x))),
        obj.total_records.map(x => JField("total_records", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbFindExperimentsResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbFindExperimentsResponse(
          // TODO: handle required
          experiments = fieldsMap.get("experiments").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbExperiment.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          total_records = fieldsMap.get("total_records").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
