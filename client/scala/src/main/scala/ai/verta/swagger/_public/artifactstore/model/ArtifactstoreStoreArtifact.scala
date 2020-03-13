// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.artifactstore.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class ArtifactstoreStoreArtifact (
  key: Option[String] = None,
  path: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ArtifactstoreStoreArtifact.toJson(this)
}

object ArtifactstoreStoreArtifact {
  def toJson(obj: ArtifactstoreStoreArtifact): JObject = {
    new JObject(
      List[Option[JField]](
        obj.key.map(x => JField("key", JString(x))),
        obj.path.map(x => JField("path", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ArtifactstoreStoreArtifact =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ArtifactstoreStoreArtifact(
          // TODO: handle required
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString),
          path = fieldsMap.get("path").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
