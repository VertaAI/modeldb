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

case class ModeldbDeleteExperimentTag (
  id: Option[String] = None,
  tag: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbDeleteExperimentTag.toJson(this)
}

object ModeldbDeleteExperimentTag {
  def toJson(obj: ModeldbDeleteExperimentTag): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.tag.map(x => JField("tag", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbDeleteExperimentTag =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbDeleteExperimentTag(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          tag = fieldsMap.get("tag").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
