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

case class ModeldbDeleteExperimentTagResponse (
  experiment: Option[ModeldbExperiment] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbDeleteExperimentTagResponse.toJson(this)
}

object ModeldbDeleteExperimentTagResponse {
  def toJson(obj: ModeldbDeleteExperimentTagResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.experiment.map(x => JField("experiment", ((x: ModeldbExperiment) => ModeldbExperiment.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbDeleteExperimentTagResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbDeleteExperimentTagResponse(
          // TODO: handle required
          experiment = fieldsMap.get("experiment").map(ModeldbExperiment.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
