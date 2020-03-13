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

case class ModeldbUpdateExperimentRunDescriptionResponse (
  experiment_run: Option[ModeldbExperimentRun] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbUpdateExperimentRunDescriptionResponse.toJson(this)
}

object ModeldbUpdateExperimentRunDescriptionResponse {
  def toJson(obj: ModeldbUpdateExperimentRunDescriptionResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.experiment_run.map(x => JField("experiment_run", ((x: ModeldbExperimentRun) => ModeldbExperimentRun.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbUpdateExperimentRunDescriptionResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbUpdateExperimentRunDescriptionResponse(
          // TODO: handle required
          experiment_run = fieldsMap.get("experiment_run").map(ModeldbExperimentRun.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
