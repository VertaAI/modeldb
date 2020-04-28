// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class ModeldbVersioningLineageEntry (
  commit_sha: Option[String] = None,
  location: Option[List[String]] = None,
  repository_id: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbVersioningLineageEntry.toJson(this)
}

object ModeldbVersioningLineageEntry {
  def toJson(obj: ModeldbVersioningLineageEntry): JObject = {
    new JObject(
      List[Option[JField]](
        obj.commit_sha.map(x => JField("commit_sha", JString(x))),
        obj.location.map(x => JField("location", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.repository_id.map(x => JField("repository_id", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbVersioningLineageEntry =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbVersioningLineageEntry(
          // TODO: handle required
          commit_sha = fieldsMap.get("commit_sha").map(JsonConverter.fromJsonString),
          location = fieldsMap.get("location").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          repository_id = fieldsMap.get("repository_id").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
