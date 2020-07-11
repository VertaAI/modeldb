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

case class ModeldbCommitArtifactPart (
  artifact_part: Option[CommonArtifactPart] = None,
  id: Option[String] = None,
  key: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbCommitArtifactPart.toJson(this)
}

object ModeldbCommitArtifactPart {
  def toJson(obj: ModeldbCommitArtifactPart): JObject = {
    new JObject(
      List[Option[JField]](
        obj.artifact_part.map(x => JField("artifact_part", ((x: CommonArtifactPart) => CommonArtifactPart.toJson(x))(x))),
        obj.id.map(x => JField("id", JString(x))),
        obj.key.map(x => JField("key", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbCommitArtifactPart =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbCommitArtifactPart(
          // TODO: handle required
          artifact_part = fieldsMap.get("artifact_part").map(CommonArtifactPart.fromJson),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
