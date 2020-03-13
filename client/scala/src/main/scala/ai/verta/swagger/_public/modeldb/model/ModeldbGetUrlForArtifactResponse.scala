// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.model.ModeldbProjectVisibility._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class ModeldbGetUrlForArtifactResponse (
  url: Option[String] = None,
  fields: Option[Map[String,String]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetUrlForArtifactResponse.toJson(this)
}

object ModeldbGetUrlForArtifactResponse {
  def toJson(obj: ModeldbGetUrlForArtifactResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.url.map(x => JField("url", JString(x))),
        obj.fields.map(x => JField("fields", ((x: Map[String,String]) => JObject(x.toList.map(kv => JField(kv._1,JString(kv._2)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetUrlForArtifactResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetUrlForArtifactResponse(
          // TODO: handle required
          url = fieldsMap.get("url").map(JsonConverter.fromJsonString),
          fields = fieldsMap.get("fields").map((x: JValue) => x match {case JObject(fields) => fields.map(kv => (kv.name, JsonConverter.fromJsonString(kv.value))).toMap; case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
