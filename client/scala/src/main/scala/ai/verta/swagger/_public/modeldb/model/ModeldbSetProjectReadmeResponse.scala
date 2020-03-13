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

case class ModeldbSetProjectReadmeResponse (
  project: Option[ModeldbProject] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbSetProjectReadmeResponse.toJson(this)
}

object ModeldbSetProjectReadmeResponse {
  def toJson(obj: ModeldbSetProjectReadmeResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.project.map(x => JField("project", ((x: ModeldbProject) => ModeldbProject.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbSetProjectReadmeResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbSetProjectReadmeResponse(
          // TODO: handle required
          project = fieldsMap.get("project").map(ModeldbProject.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
