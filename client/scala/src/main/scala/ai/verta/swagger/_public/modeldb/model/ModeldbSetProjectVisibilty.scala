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

case class ModeldbSetProjectVisibilty (
  id: Option[String] = None,
  project_visibility: Option[ModeldbProjectVisibility] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbSetProjectVisibilty.toJson(this)
}

object ModeldbSetProjectVisibilty {
  def toJson(obj: ModeldbSetProjectVisibilty): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", JString(x))),
        obj.project_visibility.map(x => JField("project_visibility", ((x: ModeldbProjectVisibility) => ModeldbProjectVisibility.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbSetProjectVisibilty =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbSetProjectVisibilty(
          // TODO: handle required
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          project_visibility = fieldsMap.get("project_visibility").map(ModeldbProjectVisibility.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
