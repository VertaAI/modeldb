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

case class ModeldbGetUrlForArtifact (
  id: Option[String] = None,
  key: Option[String] = None,
  method: Option[String] = None,
  artifact_type: Option[ArtifactTypeEnumArtifactType] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetUrlForArtifact.toJson(this)
}

object ModeldbGetUrlForArtifact {
  def toJson(obj: ModeldbGetUrlForArtifact): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.key.map(x => JField("key", JString(x))),
        obj.method.map(x => JField("method", JString(x))),
        obj.artifact_type.map(x => JField("artifact_type", ((x: ArtifactTypeEnumArtifactType) => ArtifactTypeEnumArtifactType.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetUrlForArtifact =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetUrlForArtifact(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString),
          method = fieldsMap.get("method").map(JsonConverter.fromJsonString),
          artifact_type = fieldsMap.get("artifact_type").map(ArtifactTypeEnumArtifactType.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
