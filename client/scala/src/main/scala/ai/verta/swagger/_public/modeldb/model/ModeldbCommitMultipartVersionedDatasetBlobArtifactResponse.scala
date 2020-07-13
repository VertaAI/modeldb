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

case class ModeldbCommitMultipartVersionedDatasetBlobArtifactResponse (
) extends BaseSwagger {
  def toJson(): JValue = ModeldbCommitMultipartVersionedDatasetBlobArtifactResponse.toJson(this)
}

object ModeldbCommitMultipartVersionedDatasetBlobArtifactResponse {
  def toJson(obj: ModeldbCommitMultipartVersionedDatasetBlobArtifactResponse): JObject = {
    new JObject(
      List[Option[JField]](
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbCommitMultipartVersionedDatasetBlobArtifactResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbCommitMultipartVersionedDatasetBlobArtifactResponse(
          // TODO: handle required
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
