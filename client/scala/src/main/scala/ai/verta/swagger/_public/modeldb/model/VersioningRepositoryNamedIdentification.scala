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

case class VersioningRepositoryNamedIdentification (
  name: Option[String] = None,
  workspace_name: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningRepositoryNamedIdentification.toJson(this)
}

object VersioningRepositoryNamedIdentification {
  def toJson(obj: VersioningRepositoryNamedIdentification): JObject = {
    new JObject(
      List[Option[JField]](
        obj.name.map(x => JField("name", JString(x))),
        obj.workspace_name.map(x => JField("workspace_name", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningRepositoryNamedIdentification =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningRepositoryNamedIdentification(
          // TODO: handle required
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          workspace_name = fieldsMap.get("workspace_name").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
