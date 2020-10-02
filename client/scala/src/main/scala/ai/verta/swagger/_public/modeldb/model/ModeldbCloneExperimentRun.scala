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

case class ModeldbCloneExperimentRun (
  dest_experiment_id: Option[String] = None,
  dest_experiment_run_name: Option[String] = None,
  src_experiment_run_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbCloneExperimentRun.toJson(this)
}

object ModeldbCloneExperimentRun {
  def toJson(obj: ModeldbCloneExperimentRun): JObject = {
    new JObject(
      List[Option[JField]](
        obj.dest_experiment_id.map(x => JField("dest_experiment_id", JString(x))),
        obj.dest_experiment_run_name.map(x => JField("dest_experiment_run_name", JString(x))),
        obj.src_experiment_run_id.map(x => JField("src_experiment_run_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbCloneExperimentRun =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbCloneExperimentRun(
          // TODO: handle required
          dest_experiment_id = fieldsMap.get("dest_experiment_id").map(JsonConverter.fromJsonString),
          dest_experiment_run_name = fieldsMap.get("dest_experiment_run_name").map(JsonConverter.fromJsonString),
          src_experiment_run_id = fieldsMap.get("src_experiment_run_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
