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

case class ModeldbGetProjectByNameResponse (
  project_by_user: Option[ModeldbProject] = None,
  shared_projects: Option[List[ModeldbProject]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbGetProjectByNameResponse.toJson(this)
}

object ModeldbGetProjectByNameResponse {
  def toJson(obj: ModeldbGetProjectByNameResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.project_by_user.map(x => JField("project_by_user", ((x: ModeldbProject) => ModeldbProject.toJson(x))(x))),
        obj.shared_projects.map(x => JField("shared_projects", ((x: List[ModeldbProject]) => JArray(x.map(((x: ModeldbProject) => ModeldbProject.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbGetProjectByNameResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbGetProjectByNameResponse(
          // TODO: handle required
          project_by_user = fieldsMap.get("project_by_user").map(ModeldbProject.fromJson),
          shared_projects = fieldsMap.get("shared_projects").map((x: JValue) => x match {case JArray(elements) => elements.map(ModeldbProject.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
