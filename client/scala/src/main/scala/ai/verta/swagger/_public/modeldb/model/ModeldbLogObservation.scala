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

case class ModeldbLogObservation (
  id: Option[String] = None,
  observation: Option[ModeldbObservation] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLogObservation.toJson(this)
}

object ModeldbLogObservation {
  def toJson(obj: ModeldbLogObservation): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.observation.map(x => JField("observation", ((x: ModeldbObservation) => ModeldbObservation.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLogObservation =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLogObservation(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          observation = fieldsMap.get("observation").map(ModeldbObservation.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
