// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbLogDataset (
  id: Option[String] = None,
  dataset: Option[ModeldbArtifact] = None,
  overwrite: Option[Boolean] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLogDataset.toJson(this)
}

object ModeldbLogDataset {
  def toJson(obj: ModeldbLogDataset): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.dataset.map(x => JField("dataset", ((x: ModeldbArtifact) => ModeldbArtifact.toJson(x))(x))),
        obj.overwrite.map(x => JField("overwrite", JBool(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLogDataset =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLogDataset(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          dataset = fieldsMap.get("dataset").map(ModeldbArtifact.fromJson),
          overwrite = fieldsMap.get("overwrite").map(JsonConverter.fromJsonBoolean)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
