// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.DatasetTypeEnumDatasetType._
import ai.verta.swagger._public.modeldb.model.DatasetVisibilityEnumDatasetVisibility._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.PathLocationTypeEnumPathLocationType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger.client.objects._

case class ModeldbGetCommittedVersionedDatasetBlobArtifactPartsResponse (
  artifact_parts: Option[List[CommonArtifactPart]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetCommittedVersionedDatasetBlobArtifactPartsResponse.toJson(this)
}

object ModeldbGetCommittedVersionedDatasetBlobArtifactPartsResponse {
  def toJson(obj: ModeldbGetCommittedVersionedDatasetBlobArtifactPartsResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.artifact_parts.map(x => JField("artifact_parts", ((x: List[CommonArtifactPart]) => JArray(x.map(((x: CommonArtifactPart) => CommonArtifactPart.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetCommittedVersionedDatasetBlobArtifactPartsResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetCommittedVersionedDatasetBlobArtifactPartsResponse(
          // TODO: handle required
          artifact_parts = fieldsMap.get("artifact_parts").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonArtifactPart.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
