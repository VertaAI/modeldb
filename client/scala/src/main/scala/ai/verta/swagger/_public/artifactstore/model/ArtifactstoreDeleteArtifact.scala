// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.artifactstore.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class ArtifactstoreDeleteArtifact (
  key: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ArtifactstoreDeleteArtifact.toJson(this)
}

object ArtifactstoreDeleteArtifact {
  def toJson(obj: ArtifactstoreDeleteArtifact): JObject = {
    new JObject(
      List[Option[JField]](
        obj.key.map(x => JField("key", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ArtifactstoreDeleteArtifact =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ArtifactstoreDeleteArtifact(
          // TODO: handle required
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
