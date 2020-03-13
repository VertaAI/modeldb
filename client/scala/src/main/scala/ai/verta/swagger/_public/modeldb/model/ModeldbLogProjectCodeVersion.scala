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

case class ModeldbLogProjectCodeVersion (
  id: Option[String] = None,
  code_version: Option[ModeldbCodeVersion] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbLogProjectCodeVersion.toJson(this)
}

object ModeldbLogProjectCodeVersion {
  def toJson(obj: ModeldbLogProjectCodeVersion): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.code_version.map(x => JField("code_version", ((x: ModeldbCodeVersion) => ModeldbCodeVersion.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbLogProjectCodeVersion =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbLogProjectCodeVersion(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          code_version = fieldsMap.get("code_version").map(ModeldbCodeVersion.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
