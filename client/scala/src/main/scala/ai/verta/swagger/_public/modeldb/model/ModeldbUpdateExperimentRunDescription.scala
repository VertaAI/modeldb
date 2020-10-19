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

case class ModeldbUpdateExperimentRunDescription (
  description: Option[String] = None,
  id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbUpdateExperimentRunDescription.toJson(this)
}

object ModeldbUpdateExperimentRunDescription {
  def toJson(obj: ModeldbUpdateExperimentRunDescription): JObject = {
    new JObject(
      List[Option[JField]](
        obj.description.map(x => JField("description", JString(x))),
        obj.id.map(x => JField("id", JString(x)))
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
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
