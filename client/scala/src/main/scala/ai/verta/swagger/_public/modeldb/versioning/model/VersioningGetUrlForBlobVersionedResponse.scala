// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.versioning.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.versioning.model.RepositoryVisibilityEnumRepositoryVisibility._
import ai.verta.swagger._public.modeldb.versioning.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.versioning.model.VersioningBlobType._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningGetUrlForBlobVersionedResponse (
  multipart_upload_ok: Option[Boolean] = None,
  url: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningGetUrlForBlobVersionedResponse.toJson(this)
}

object VersioningGetUrlForBlobVersionedResponse {
  def toJson(obj: VersioningGetUrlForBlobVersionedResponse): JObject = {
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

  def fromJson(value: JValue): VersioningGetUrlForBlobVersionedResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningGetUrlForBlobVersionedResponse(
          // TODO: handle required
          multipart_upload_ok = fieldsMap.get("multipart_upload_ok").map(JsonConverter.fromJsonBoolean),
          url = fieldsMap.get("url").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
