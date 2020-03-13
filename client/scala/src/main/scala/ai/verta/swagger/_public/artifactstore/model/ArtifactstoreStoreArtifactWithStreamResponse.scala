// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.artifactstore.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class ArtifactstoreStoreArtifactWithStreamResponse (
  cloud_file_key: Option[String] = None,
  cloud_file_path: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ArtifactstoreStoreArtifactWithStreamResponse.toJson(this)
}

object ArtifactstoreStoreArtifactWithStreamResponse {
  def toJson(obj: ArtifactstoreStoreArtifactWithStreamResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.cloud_file_key.map(x => JField("cloud_file_key", JString(x))),
        obj.cloud_file_path.map(x => JField("cloud_file_path", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ArtifactstoreStoreArtifactWithStreamResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ArtifactstoreStoreArtifactWithStreamResponse(
          // TODO: handle required
          cloud_file_key = fieldsMap.get("cloud_file_key").map(JsonConverter.fromJsonString),
          cloud_file_path = fieldsMap.get("cloud_file_path").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
