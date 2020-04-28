// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger.client.objects._

case class ModeldbLineageEntry (
  blob: Option[ModeldbVersioningLineageEntry] = None,
  experiment_run: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLineageEntry.toJson(this)
}

object ModeldbLineageEntry {
  def toJson(obj: ModeldbLineageEntry): JObject = {
    new JObject(
      List[Option[JField]](
        obj.blob.map(x => JField("blob", ((x: ModeldbVersioningLineageEntry) => ModeldbVersioningLineageEntry.toJson(x))(x))),
        obj.experiment_run.map(x => JField("experiment_run", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLineageEntry =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLineageEntry(
          // TODO: handle required
          blob = fieldsMap.get("blob").map(ModeldbVersioningLineageEntry.fromJson),
          experiment_run = fieldsMap.get("experiment_run").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
