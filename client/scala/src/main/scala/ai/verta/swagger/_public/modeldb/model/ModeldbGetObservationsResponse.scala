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

case class ModeldbGetObservationsResponse (
  observations: Option[List[ModeldbObservation]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetObservationsResponse.toJson(this)
}

object ModeldbGetObservationsResponse {
  def toJson(obj: ModeldbGetObservationsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.observations.map(x => JField("observations", ((x: List[ModeldbObservation]) => JArray(x.map(((x: ModeldbObservation) => ModeldbObservation.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetObservationsResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetObservationsResponse(
          // TODO: handle required
          observations = fieldsMap.get("observations").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbObservation.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
