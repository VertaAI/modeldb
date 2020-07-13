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

case class ModeldbGetUrlForDatasetBlobVersionedResponse (
  multipart_upload_ok: Option[Boolean] = None,
  url: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetUrlForDatasetBlobVersionedResponse.toJson(this)
}

object ModeldbGetUrlForDatasetBlobVersionedResponse {
  def toJson(obj: ModeldbGetUrlForDatasetBlobVersionedResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.multipart_upload_ok.map(x => JField("multipart_upload_ok", JBool(x))),
        obj.url.map(x => JField("url", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetUrlForDatasetBlobVersionedResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetUrlForDatasetBlobVersionedResponse(
          // TODO: handle required
          multipart_upload_ok = fieldsMap.get("multipart_upload_ok").map(JsonConverter.fromJsonBoolean),
          url = fieldsMap.get("url").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
