// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.artifactstore.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class ArtifactstoreGetArtifactResponse (
  contents: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ArtifactstoreGetArtifactResponse.toJson(this)
}

object ArtifactstoreGetArtifactResponse {
  def toJson(obj: ArtifactstoreGetArtifactResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.contents.map(x => JField("contents", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ArtifactstoreGetArtifactResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ArtifactstoreGetArtifactResponse(
          // TODO: handle required
          contents = fieldsMap.get("contents").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
