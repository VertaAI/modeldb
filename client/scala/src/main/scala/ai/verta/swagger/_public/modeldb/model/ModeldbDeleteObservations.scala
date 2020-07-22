// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger.client.objects._

case class ModeldbDeleteObservations (
  delete_all: Option[Boolean] = None,
  id: Option[String] = None,
  observation_keys: Option[List[String]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbDeleteObservations.toJson(this)
}

object ModeldbDeleteObservations {
  def toJson(obj: ModeldbDeleteObservations): JObject = {
    new JObject(
      List[Option[JField]](
        obj.delete_all.map(x => JField("delete_all", JBool(x))),
        obj.id.map(x => JField("id", JString(x))),
        obj.observation_keys.map(x => JField("observation_keys", ((x: List[String]) => JArray(x.map(JString)))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbDeleteObservations =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbDeleteObservations(
          // TODO: handle required
          delete_all = fieldsMap.get("delete_all").map(JsonConverter.fromJsonBoolean),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          observation_keys = fieldsMap.get("observation_keys").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
