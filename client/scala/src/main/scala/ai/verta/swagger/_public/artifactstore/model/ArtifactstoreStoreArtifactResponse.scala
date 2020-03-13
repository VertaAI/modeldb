// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.artifactstore.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class ArtifactstoreStoreArtifactResponse (
  artifact_store_key: Option[String] = None,
  artifact_store_path: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ArtifactstoreStoreArtifactResponse.toJson(this)
}

object ArtifactstoreStoreArtifactResponse {
  def toJson(obj: ArtifactstoreStoreArtifactResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.artifact_store_key.map(x => JField("artifact_store_key", JString(x))),
        obj.artifact_store_path.map(x => JField("artifact_store_path", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ArtifactstoreStoreArtifactResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ArtifactstoreStoreArtifactResponse(
          // TODO: handle required
          artifact_store_key = fieldsMap.get("artifact_store_key").map(JsonConverter.fromJsonString),
          artifact_store_path = fieldsMap.get("artifact_store_path").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
