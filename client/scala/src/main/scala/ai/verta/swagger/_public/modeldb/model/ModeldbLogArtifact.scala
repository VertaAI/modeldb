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

case class ModeldbLogArtifact (
  id: Option[String] = None,
  artifact: Option[ModeldbArtifact] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLogArtifact.toJson(this)
}

object ModeldbLogArtifact {
  def toJson(obj: ModeldbLogArtifact): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.artifact.map(x => JField("artifact", ((x: ModeldbArtifact) => ModeldbArtifact.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLogArtifact =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLogArtifact(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          artifact = fieldsMap.get("artifact").map(ModeldbArtifact.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
