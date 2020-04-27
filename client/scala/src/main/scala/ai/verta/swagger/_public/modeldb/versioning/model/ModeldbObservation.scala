// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.versioning.model.RepositoryVisibilityEnumRepositoryVisibility._
import ai.verta.swagger._public.modeldb.versioning.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.versioning.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.versioning.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.versioning.model.VersioningBlobType._
import ai.verta.swagger.client.objects._

case class ModeldbObservation (
  artifact: Option[ModeldbArtifact] = None,
  attribute: Option[CommonKeyValue] = None,
  timestamp: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbObservation.toJson(this)
}

object ModeldbObservation {
  def toJson(obj: ModeldbObservation): JObject = {
    new JObject(
      List[Option[JField]](
        obj.artifact.map(x => JField("artifact", ((x: ModeldbArtifact) => ModeldbArtifact.toJson(x))(x))),
        obj.attribute.map(x => JField("attribute", ((x: CommonKeyValue) => CommonKeyValue.toJson(x))(x))),
        obj.timestamp.map(x => JField("timestamp", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbObservation =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbObservation(
          // TODO: handle required
          artifact = fieldsMap.get("artifact").map(ModeldbArtifact.fromJson),
          attribute = fieldsMap.get("attribute").map(CommonKeyValue.fromJson),
          timestamp = fieldsMap.get("timestamp").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
