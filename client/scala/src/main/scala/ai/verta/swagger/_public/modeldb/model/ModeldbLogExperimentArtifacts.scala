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

case class ModeldbLogExperimentArtifacts (
  id: Option[String] = None,
  artifacts: Option[List[ModeldbArtifact]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLogExperimentArtifacts.toJson(this)
}

object ModeldbLogExperimentArtifacts {
  def toJson(obj: ModeldbLogExperimentArtifacts): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.artifacts.map(x => JField("artifacts", ((x: List[ModeldbArtifact]) => JArray(x.map(((x: ModeldbArtifact) => ModeldbArtifact.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLogExperimentArtifacts =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLogExperimentArtifacts(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          artifacts = fieldsMap.get("artifacts").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbArtifact.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
