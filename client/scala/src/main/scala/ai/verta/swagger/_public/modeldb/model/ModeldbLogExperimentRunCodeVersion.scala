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

case class ModeldbLogExperimentRunCodeVersion (
  id: Option[String] = None,
  code_version: Option[ModeldbCodeVersion] = None,
  overwrite: Option[Boolean] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLogExperimentRunCodeVersion.toJson(this)
}

object ModeldbLogExperimentRunCodeVersion {
  def toJson(obj: ModeldbLogExperimentRunCodeVersion): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.code_version.map(x => JField("code_version", ((x: ModeldbCodeVersion) => ModeldbCodeVersion.toJson(x))(x))),
        obj.overwrite.map(x => JField("overwrite", JBool(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLogExperimentRunCodeVersion =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLogExperimentRunCodeVersion(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          code_version = fieldsMap.get("code_version").map(ModeldbCodeVersion.fromJson),
          overwrite = fieldsMap.get("overwrite").map(JsonConverter.fromJsonBoolean)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
