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

case class ModeldbSetParentExperimentRunId (
  experiment_run_id: Option[String] = None,
  parent_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbSetParentExperimentRunId.toJson(this)
}

object ModeldbSetParentExperimentRunId {
  def toJson(obj: ModeldbSetParentExperimentRunId): JObject = {
    new JObject(
      List[Option[JField]](
        obj.experiment_run_id.map(x => JField("experiment_run_id", JString(x))),
        obj.parent_id.map(x => JField("parent_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbSetParentExperimentRunId =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbSetParentExperimentRunId(
          // TODO: handle required
          experiment_run_id = fieldsMap.get("experiment_run_id").map(JsonConverter.fromJsonString),
          parent_id = fieldsMap.get("parent_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
