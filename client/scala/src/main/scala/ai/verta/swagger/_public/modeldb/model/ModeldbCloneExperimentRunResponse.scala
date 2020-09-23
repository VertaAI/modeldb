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

case class ModeldbCloneExperimentRunResponse (
  run: Option[ModeldbExperimentRun] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbCloneExperimentRunResponse.toJson(this)
}

object ModeldbCloneExperimentRunResponse {
  def toJson(obj: ModeldbCloneExperimentRunResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.run.map(x => JField("run", ((x: ModeldbExperimentRun) => ModeldbExperimentRun.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbCloneExperimentRunResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbCloneExperimentRunResponse(
          // TODO: handle required
          run = fieldsMap.get("run").map(ModeldbExperimentRun.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
