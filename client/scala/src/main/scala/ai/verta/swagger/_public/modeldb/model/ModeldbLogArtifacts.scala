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

case class ModeldbLogArtifacts (
  artifacts: Option[List[CommonArtifact]] = None,
  id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLogArtifacts.toJson(this)
}

object ModeldbLogArtifacts {
  def toJson(obj: ModeldbLogArtifacts): JObject = {
    new JObject(
      List[Option[JField]](
        obj.artifacts.map(x => JField("artifacts", ((x: List[CommonArtifact]) => JArray(x.map(((x: CommonArtifact) => CommonArtifact.toJson(x)))))(x))),
        obj.id.map(x => JField("id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLogArtifacts =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLogArtifacts(
          // TODO: handle required
          artifacts = fieldsMap.get("artifacts").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonArtifact.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
