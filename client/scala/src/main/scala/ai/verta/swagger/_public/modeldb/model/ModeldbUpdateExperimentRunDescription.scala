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

case class ModeldbUpdateExperimentRunDescription (
  id: Option[String] = None,
  description: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbUpdateExperimentRunDescription.toJson(this)
}

object ModeldbUpdateExperimentRunDescription {
  def toJson(obj: ModeldbUpdateExperimentRunDescription): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.description.map(x => JField("description", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbUpdateExperimentRunDescription =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbUpdateExperimentRunDescription(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
