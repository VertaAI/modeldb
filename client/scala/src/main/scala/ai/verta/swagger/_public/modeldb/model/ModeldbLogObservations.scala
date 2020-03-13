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

case class ModeldbLogObservations (
  id: Option[String] = None,
  observations: Option[List[ModeldbObservation]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLogObservations.toJson(this)
}

object ModeldbLogObservations {
  def toJson(obj: ModeldbLogObservations): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.observations.map(x => JField("observations", ((x: List[ModeldbObservation]) => JArray(x.map(((x: ModeldbObservation) => ModeldbObservation.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLogObservations =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLogObservations(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          observations = fieldsMap.get("observations").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbObservation.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
