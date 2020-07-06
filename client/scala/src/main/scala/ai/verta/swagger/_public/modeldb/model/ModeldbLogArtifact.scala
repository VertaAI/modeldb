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

case class ModeldbLogArtifact (
  artifact: Option[CommonArtifact] = None,
  id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLogArtifact.toJson(this)
}

object ModeldbLogArtifact {
  def toJson(obj: ModeldbLogArtifact): JObject = {
    new JObject(
      List[Option[JField]](
        obj.artifact.map(x => JField("artifact", ((x: CommonArtifact) => CommonArtifact.toJson(x))(x))),
        obj.id.map(x => JField("id", JString(x)))
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
          artifact = fieldsMap.get("artifact").map(CommonArtifact.fromJson),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
