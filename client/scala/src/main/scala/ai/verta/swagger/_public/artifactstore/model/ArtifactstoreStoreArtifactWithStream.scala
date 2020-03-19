// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.artifactstore.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class ArtifactstoreStoreArtifactWithStream (
  client_file: Option[String] = None,
  key: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ArtifactstoreStoreArtifactWithStream.toJson(this)
}

object ArtifactstoreStoreArtifactWithStream {
  def toJson(obj: ArtifactstoreStoreArtifactWithStream): JObject = {
    new JObject(
      List[Option[JField]](
        obj.client_file.map(x => JField("client_file", JString(x))),
        obj.key.map(x => JField("key", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ArtifactstoreStoreArtifactWithStream =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ArtifactstoreStoreArtifactWithStream(
          // TODO: handle required
          client_file = fieldsMap.get("client_file").map(JsonConverter.fromJsonString),
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
