// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.model.ModeldbProjectVisibility._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbLastModifiedExperimentRunSummary (
  name: Option[String] = None,
  last_updated_time: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLastModifiedExperimentRunSummary.toJson(this)
}

object ModeldbLastModifiedExperimentRunSummary {
  def toJson(obj: ModeldbLastModifiedExperimentRunSummary): JObject = {
    new JObject(
      List[Option[JField]](
        obj.name.map(x => JField("name", JString(x))),
        obj.last_updated_time.map(x => JField("last_updated_time", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLastModifiedExperimentRunSummary =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLastModifiedExperimentRunSummary(
          // TODO: handle required
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          last_updated_time = fieldsMap.get("last_updated_time").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
