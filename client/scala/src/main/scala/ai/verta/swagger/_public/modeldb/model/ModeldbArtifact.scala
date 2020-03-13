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

case class ModeldbArtifact (
  key: Option[String] = None,
  path: Option[String] = None,
  path_only: Option[Boolean] = None,
  artifact_type: Option[ArtifactTypeEnumArtifactType] = None,
  linked_artifact_id: Option[String] = None,
  filename_extension: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbArtifact.toJson(this)
}

object ModeldbArtifact {
  def toJson(obj: ModeldbArtifact): JObject = {
    new JObject(
      List[Option[JField]](
        obj.key.map(x => JField("key", JString(x))),
        obj.path.map(x => JField("path", JString(x))),
        obj.path_only.map(x => JField("path_only", JBool(x))),
        obj.artifact_type.map(x => JField("artifact_type", ((x: ArtifactTypeEnumArtifactType) => ArtifactTypeEnumArtifactType.toJson(x))(x))),
        obj.linked_artifact_id.map(x => JField("linked_artifact_id", JString(x))),
        obj.filename_extension.map(x => JField("filename_extension", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbArtifact =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbArtifact(
          // TODO: handle required
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString),
          path = fieldsMap.get("path").map(JsonConverter.fromJsonString),
          path_only = fieldsMap.get("path_only").map(JsonConverter.fromJsonBoolean),
          artifact_type = fieldsMap.get("artifact_type").map(ArtifactTypeEnumArtifactType.fromJson),
          linked_artifact_id = fieldsMap.get("linked_artifact_id").map(JsonConverter.fromJsonString),
          filename_extension = fieldsMap.get("filename_extension").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
